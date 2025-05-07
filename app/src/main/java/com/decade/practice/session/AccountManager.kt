package com.decade.practice.session

import android.app.Application
import androidx.room.withTransaction
import com.decade.practice.PreferencesStore
import com.decade.practice.authentication.AuthenticationEvent
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.authentication.Authenticator
import com.decade.practice.authentication.LoginEvent
import com.decade.practice.authentication.LogoutEvent
import com.decade.practice.authentication.UnAuthorizedEvent
import com.decade.practice.createDatabase
import com.decade.practice.database.AccountDatabase
import com.decade.practice.database.getAccount
import com.decade.practice.database.insertUser
import com.decade.practice.database.save
import com.decade.practice.database.saveChats
import com.decade.practice.database.saveEdges
import com.decade.practice.database.saveEvents
import com.decade.practice.database.saveUsers
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ApplicationEventPublisher
import com.decade.practice.model.domain.AccountEntry
import com.decade.practice.model.domain.LocalEdge
import com.decade.practice.model.domain.RemoteEdge
import com.decade.practice.model.domain.User
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AccountManager @Inject constructor(
      private val builder: AccountComponentBuilder,
      private val eventPublisher: ApplicationEventPublisher,
      private val application: Application,
      private val preferencesStore: PreferencesStore,
      private val authenticator: Authenticator,
) : ApplicationEventListener, AccountRepository {

      private var accountSession: AccountSession? = null

      override val currentSession: Session?
            get() = if (accountSession == null) null else ProxySession(accountSession!!)

      init {
            val account = preferencesStore.currentAccount
            if (account != null) {
                  val database = application.createDatabase(account.username)
                  onAccountLoggedIn(account, database)
            }
      }

      private suspend fun logIn(entry: AccountEntry) {
            val account = entry.account
            val database = application.createDatabase(account.username)
            withContext(Dispatchers.IO) {
                  val snapshotList = entry.chatSnapshots
                  val userList = entry.chatSnapshots.map { it.conversation.partner }
                  val localEdges = mutableListOf<LocalEdge>()
                  val remoteEdges = mutableListOf<RemoteEdge>()
                  val chatList = entry.chatSnapshots.map {
                        it.conversation.chat
                  }
                  chatList.forEachIndexed { index, chat ->
                        if (index != 0) {
                              val prev = chatList[index - 1].identifier
                              localEdges.add(LocalEdge(prev, chat.identifier))
                              remoteEdges.add(RemoteEdge(prev, chat.identifier))
                        }
                  }

                  val eventList = entry.chatSnapshots.flatMap { it.eventList }
                  eventList.forEach { event ->
                        event.committed = true
                        event.receiveTime = event.createdTime
                  }

                  database.withTransaction {
                        database.save(account)
                        database.insertUser(account.user)
                        if (snapshotList.isEmpty())
                              return@withTransaction

                        database.saveUsers(userList)

                        database.saveChats(chatList)
                        database.saveEdges(localEdges, remoteEdges)
                        database.saveEvents(eventList)
                  }
            }
            onAccountLoggedIn(account.user, database)
      }

      @Throws(AuthenticationException::class)
      override suspend fun logIn(username: String, password: String): Session {
            assert(!hasSession) {
                  "A session is active"
            }
            val entry = authenticator.signIn(username, password)
            logIn(entry)
            return currentSession!!
      }

      @Throws(AuthenticationException::class)
      override suspend fun logIn(accessToken: String): Session {
            assert(!hasSession) {
                  "A session is active"
            }
            val entry = authenticator.signInOAuth2(accessToken)
            logIn(entry)
            return currentSession!!
      }

      override suspend fun logOut() {
            assert(hasSession) {
                  "No session active"
            }
            val session = (accountSession as AccountSession)
            val account = session.database.getAccount()
            authenticator.signOut(account)
            session.close()
            onAccountLoggedOut(session)
      }


      private fun onAccountLoggedIn(account: User, database: AccountDatabase) {
            accountSession = builder.createSession(account, database)
            eventPublisher.publish(LoginEvent(currentSession!!))
      }

      private fun onAccountLoggedOut(session: AccountSession) {
            application.deleteDatabase("database_${session.account.username}")
            accountSession = null
            preferencesStore.currentAccount = null
            eventPublisher.publish(LogoutEvent(session))
      }

      override fun supportsEventType(eventType: Class<out ApplicationEvent>) =
            eventType.isAssignableFrom(AuthenticationEvent::class.java)


      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            if (applicationEvent is UnAuthorizedEvent) {
                  val session = accountSession ?: return
                  if (applicationEvent.account == session.account)
                        logOut()
            }
      }
}


@Module
@InstallIn(SingletonComponent::class)
abstract class AccountRepoModule {
      @Binds
      abstract fun accountRepo(accountManager: AccountManager): AccountRepository
}
