package com.decade.practice.session.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.hasKeyWithValueOfType
import com.decade.practice.ChannelListener
import com.decade.practice.INBOUND_CHANNEL
import com.decade.practice.ListenableMessageChannel
import com.decade.practice.OUTBOUND_CHANNEL
import com.decade.practice.authentication.UnAuthorizedEvent
import com.decade.practice.database.AccountDatabase
import com.decade.practice.database.dao.edgeFrom
import com.decade.practice.database.dao.edgeTo
import com.decade.practice.database.getAccount
import com.decade.practice.database.save
import com.decade.practice.database.saveChat
import com.decade.practice.database.saveEvent
import com.decade.practice.database.saveUser
import com.decade.practice.endpoints.HttpContext
import com.decade.practice.endpoints.eventCall
import com.decade.practice.event.ApplicationEventPublisher
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.LocalEdge
import com.decade.practice.model.domain.RemoteEdge
import com.decade.practice.model.domain.SEEN
import com.decade.practice.model.domain.User
import com.decade.practice.model.domain.isMessage
import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import com.decade.practice.session.sync.work.ACCOUNT_PARAM
import com.decade.practice.session.sync.work.NETWORK_EXCEPTION_FLAG
import com.decade.practice.session.sync.work.UNAUTHORIZED_EXCEPTION_CODE
import com.decade.practice.session.sync.work.UploadWorker
import com.decade.practice.session.sync.work.workTag
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.ceil

@Module
@InstallIn(AccountComponent::class)
abstract class SynchronizerModule {
      @Binds
      @IntoSet
      abstract fun accountLifeCycle(synchronizer: Synchronizer): AccountLifecycle
}

@AccountScope
class Synchronizer @Inject constructor(
      @Named(INBOUND_CHANNEL) private val inChannel: ListenableMessageChannel,
      @Named(OUTBOUND_CHANNEL) private val outChannel: ListenableMessageChannel,
      private val account: User,
      private val context: Context,
      private val database: AccountDatabase,
      private val eventPublisher: ApplicationEventPublisher,
      private val sessionScope: CoroutineScope,
      httpContext: HttpContext
) : AccountLifecycle, ChannelListener {

      private lateinit var workManager: WorkManager
      private val eventChannel = Channel<ChatEvent>()
      private val eventDao = database.eventDao()
      private val localEdgeDao = database.localEdgeDao()
      private val remoteEdgeDao = database.remoteEdgeDao()
      private val retrofit = httpContext.retrofit
      private lateinit var uploadJob: Job
      private lateinit var synchronizeJob: Job


      override suspend fun onStart() {
            UploadWorker.database = database
            workManager = WorkManager.getInstance(context)
            inChannel.register(this)
            outChannel.register(this)
      }

      override suspend fun onResume() {
            synchronizeJob = sessionScope.launch {
                  supervisorScope {
                        while (true) {
                              val event = eventChannel.receive()
                              if (event.sending) {
                                    saveLocal(event)
                                    scheduleUpload()
                              } else {
                                    saveRemote(event)
                              }
                        }
                  }
            }
            uploadJob = sessionScope.launch {
                  workManager.getWorkInfosByTagFlow(account.workTag())
                        .collect(UploadCollector())
            }
            scheduleUpload()
      }

      override fun onMessage(chatEvent: ChatEvent) {
            if (chatEvent.sending) {
                  val event = chatEvent.copy()
                  event.chat = chatEvent.chat
                  event.owner = chatEvent.owner
                  event.partner = chatEvent.partner
                  assert(chatEvent.id == event.id)
                  eventPublisher.publish(event)
            }
            sessionScope.launch {
                  eventChannel.send(chatEvent)
            }
      }


      override suspend fun onLogout() {
            inChannel.unRegister(this)
            outChannel.unRegister(this)
            synchronizeJob.cancel()
            uploadJob.cancel()
            workManager.cancelAllWorkByTag(account.workTag()).await()
      }

      private suspend fun saveLocal(event: ChatEvent) {
            val chat = event.chat
            val partner = event.partner
            delay(20) // many reason
            event.receiveTime = System.currentTimeMillis()
            if (event.eventType == SEEN
                  && event.sending
                  && eventDao.findUnSentSeen() != null
            )
                  return

            val saved = withContext(Dispatchers.IO) {
                  database.withTransaction {
                        event.committed = true
                        val account = database.getAccount()
                        val syncContext = account.syncContext
                        if (event.eventVersion != null) {
                              syncContext.incVersion()
                              assert(syncContext.eventVersion == event.eventVersion)
                        }

                        database.save(account)
                        database.saveChat(chat)
                        database.saveUser(partner)
                        database.saveEvent(event)

                        if (event.isMessage()) {
                              val localHead = localEdgeDao.head().chat
                              val remoteHead = remoteEdgeDao.head().chat

                              if (localHead != chat) {
                                    val fromChat = localEdgeDao.edgeFrom(chat)
                                    val toChat = localEdgeDao.edgeTo(chat)
                                    if (fromChat != null && toChat != null) {
                                          localEdgeDao.insert(LocalEdge(toChat.from, fromChat.to))
                                    } else if (toChat != null) {
                                          localEdgeDao.delete(toChat)
                                    }
                                    localEdgeDao.insert(LocalEdge(chat, localHead))
                              }

                              if (remoteHead != chat) {
                                    val fromChat = remoteEdgeDao.edgeFrom(chat)
                                    val toChat = remoteEdgeDao.edgeTo(chat)
                                    if (fromChat != null && toChat != null) {
                                          remoteEdgeDao.insert(RemoteEdge(toChat.from, fromChat.to))
                                    } else if (toChat != null) {
                                          remoteEdgeDao.delete(toChat)
                                    }
                                    remoteEdgeDao.insert(RemoteEdge(chat, remoteHead))
                              }
                        }
                        event
                  }
            }

            if (saved.committed)
                  eventPublisher.publish(saved)
      }

      private suspend fun saveRemote(event: ChatEvent) {
            val eventVersion = event.eventVersion!!
            val accountVersion = database.getAccount().syncContext.eventVersion
            if (eventVersion <= accountVersion)
                  return
            var eventList = mutableListOf(event)
            // TODO: multiple request
            try {
                  if (eventVersion > accountVersion + 1) {
                        val additions = withContext(Dispatchers.IO) {
                              supervisorScope {
                                    val limit = 20
                                    val n = ceil((eventVersion - accountVersion - 1).toFloat() / limit).toInt()
                                    val jobs = (0..<n).map { index ->
                                          async {
                                                retrofit.eventCall().list(eventVersion - 1 - index * limit)
                                          }
                                    }
                                    jobs.awaitAll().flatten()
                              }
                        }
                        eventList.addAll(additions)
                        eventList = eventList.subList(0, eventVersion - accountVersion)
                  }
            } catch (e: IOException) {
                  e.printStackTrace()
                  return
            }
            eventList.forEach {
                  saveLocal(it)
            }
      }

      private fun scheduleUpload() {
            workManager.enqueueUploadWorkRequest(account)
      }

      inner class UploadCollector : FlowCollector<List<WorkInfo>> {
            override suspend fun emit(list: List<WorkInfo>) {
                  if (list.isNotEmpty()) {
                        val result = list.first()
                        when (result.state) {
                              WorkInfo.State.SUCCEEDED -> {
                                    if (eventDao.hasUnSent())
                                          scheduleUpload()
                              }

                              WorkInfo.State.FAILED -> {
                                    val output = result.outputData
                                    if (output.hasKeyWithValueOfType<Boolean>(NETWORK_EXCEPTION_FLAG)) {
                                          scheduleUpload()
                                    } else {
                                          val code = output.getInt(UNAUTHORIZED_EXCEPTION_CODE, 0)
                                          if (code != 0)
                                                eventPublisher.publish(UnAuthorizedEvent(account, code))
                                    }
                              }

                              else -> {}
                        }
                  }
            }
      }

}

private fun WorkManager.enqueueUploadWorkRequest(account: User) {
      val tag = account.workTag()
      val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
      val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag(tag)
            .setInputData(Data.Builder().putString(ACCOUNT_PARAM, account.username).build())
            .setConstraints(constraints)
            .build()
      enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, workRequest)
}

