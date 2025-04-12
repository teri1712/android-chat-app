package com.decade.practice.message

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.hasKeyWithValueOfType
import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import com.decade.practice.authentication.UnAuthorizedEvent
import com.decade.practice.cacheFile
import com.decade.practice.db.AccountDatabase
import com.decade.practice.db.dao.EventDao
import com.decade.practice.db.dao.edgeFrom
import com.decade.practice.db.dao.edgeTo
import com.decade.practice.db.getAccount
import com.decade.practice.db.save
import com.decade.practice.db.saveChat
import com.decade.practice.db.saveEvent
import com.decade.practice.db.saveUser
import com.decade.practice.event.ApplicationEventPublisher
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.ICON
import com.decade.practice.model.IMAGE
import com.decade.practice.model.LocalEdge
import com.decade.practice.model.RemoteEdge
import com.decade.practice.model.SEEN
import com.decade.practice.model.TEXT
import com.decade.practice.model.User
import com.decade.practice.model.isMessage
import com.decade.practice.net.HttpContext
import com.decade.practice.net.api.eventCall
import com.decade.practice.retrofit
import com.google.gson.Gson
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
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
    @Named(INBOUND_CHANNEL) private val inChannel: ListenableMessageQueue,
    @Named(OUTBOUND_CHANNEL) private val outChannel: ListenableMessageQueue,
    private val account: User,
    private val context: Context,
    private val database: AccountDatabase,
    private val eventPublisher: ApplicationEventPublisher,
    private val sessionScope: CoroutineScope,
    httpContext: HttpContext
) : AccountLifecycle, QueueListener {

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

private const val WORK_GROUP_PREFIX = "MESSAGE_WORK_GROUP"
private const val ACCOUNT_PARAM = "ACCOUNT_USERNAME"
private const val NETWORK_EXCEPTION_FLAG = "NETWORK_EXCEPTION"
private const val UNAUTHORIZED_EXCEPTION_CODE = "UNAUTHORIZED_EXCEPTION_CODE"

private fun User.workTag(): String {
    return WORK_GROUP_PREFIX + "_" + username
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

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val database: AccountDatabase = UploadWorker.database
    private val retrofit = context.retrofit()
    private val eventDao: EventDao = database.eventDao()
    private val gson = Gson()


    override suspend fun doWork(): Result {
        try {
            val eventCall = retrofit.eventCall()
            val credential = database.getAccount().credential
            val tokenHeader = "Bearer " + credential.accessToken
            var _event: ChatEvent = eventDao.findUnSent() ?: return Result.success()

            withContext(Dispatchers.IO) {
                when (_event.eventType) {
                    TEXT -> eventCall.sendText(tokenHeader, _event)
                    SEEN -> eventCall.sendSeen(tokenHeader, _event)
                    ICON -> eventCall.sendIcon(tokenHeader, _event)
                    IMAGE -> {
                        // TODO: Handle Local URI/ Content Resolver
                        val imageEvent = _event.imageEvent!!
                        val file = applicationContext.cacheFile(imageEvent.uri.toUri())
                        val requestFile = RequestBody.create(MultipartBody.FORM, file)
                        val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                        val json = gson.toJson(_event)
                        val eventPart = RequestBody.create("application/json".toMediaType(), json)
                        val result = eventCall.sendImage(tokenHeader, imagePart, eventPart)
                        result
                    }

                    else -> {

                    }
                }
            }

        } catch (httpe: HttpException) {
            httpe.printStackTrace()
            if (httpe.code() == 401 || httpe.code() == 403) {
                return Result.failure(
                    Data.Builder()
                        .putInt(UNAUTHORIZED_EXCEPTION_CODE, httpe.code())
                        .build()
                )
            }
            throw httpe
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            return Result.failure(
                Data.Builder()
                    .putBoolean(NETWORK_EXCEPTION_FLAG, true)
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
        return Result.success()
    }

    companion object {

        @Volatile
        lateinit var database: AccountDatabase

    }
}

