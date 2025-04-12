package com.decade.practice.session

import com.decade.practice.components.MessageService
import com.decade.practice.db.AccountDatabase
import com.decade.practice.model.User
import com.decade.practice.net.HttpContext
import com.decade.practice.net.OnlineClient
import com.decade.practice.repository.ConversationRepository
import com.decade.practice.repository.EventRepository
import com.decade.practice.repository.MessageRepository
import com.decade.practice.repository.OnlineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class Session {
    abstract val messageService: MessageService
    abstract val account: User
    abstract val onlineClient: OnlineClient
    abstract val accountManager: AccountManager
    abstract val coroutineScope: CoroutineScope
    abstract val httpContext: HttpContext
    abstract val eventRepository: EventRepository
    abstract val conversationRepository: ConversationRepository
    abstract val persistentContext: PersistentContext
    abstract val messageRepoFactory: MessageRepository.Factory
    abstract val onlineRepository: OnlineRepository
    abstract val ready: Boolean
}

// Fuck kotlin Encapsulation
class ProxySession(
    accountSession: AccountSession,
) : Session() {
    override val onlineRepository: OnlineRepository = accountSession.onlineRepository
    override val messageService: MessageService = accountSession.messageService
    override val account: User = accountSession.account
    override val onlineClient: OnlineClient = accountSession.onlineClient
    override val accountManager: AccountManager = accountSession.accountManager
    override val coroutineScope: CoroutineScope = accountSession.coroutineScope
    override val httpContext: HttpContext = accountSession.httpContext
    override val conversationRepository: ConversationRepository = accountSession.conversationRepository
    override val messageRepoFactory: MessageRepository.Factory = accountSession.messageRepoFactory
    override val persistentContext: PersistentContext = accountSession.persistentContext
    override val ready: Boolean = accountSession.ready
    override val eventRepository: EventRepository = accountSession.eventRepository
}

@AccountScope
class AccountSession @Inject constructor(
    override val account: User,
    override val onlineClient: OnlineClient,
    override val accountManager: AccountManager,
    override val coroutineScope: CoroutineScope,
    override val httpContext: HttpContext,
    override val messageService: MessageService,
    override val conversationRepository: ConversationRepository,
    override val messageRepoFactory: MessageRepository.Factory,
    override val persistentContext: PersistentContext,
    override val onlineRepository: OnlineRepository,
    override val eventRepository: EventRepository,
    private val accountLifecycles: Set<@JvmSuppressWildcards AccountLifecycle>,
    val database: AccountDatabase,
) : Session() {

    override var ready: Boolean = false
        private set

    init {
        coroutineScope.launch {
            accountLifecycles.forEach {
                it.onStart()
            }
            accountLifecycles.forEach {
                it.onResume()
            }
            ready = true
        }
    }

    suspend fun close() {
        if (!coroutineScope.isActive) {
            error("Session closed")
        }
        coroutineScope.cancel()
        accountLifecycles.forEach {
            it.onLogout()
        }
    }
}
