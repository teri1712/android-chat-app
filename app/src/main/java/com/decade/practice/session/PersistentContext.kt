package com.decade.practice.session

import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.ChatSnapshot
import com.decade.practice.model.Conversation
import com.decade.practice.model.Online
import com.decade.practice.model.User
import com.decade.practice.repository.ChatRepository
import com.decade.practice.repository.EventRepository
import com.decade.practice.repository.OnlineRepository
import com.decade.practice.view.viewmodel.Message
import com.decade.practice.view.viewmodel.toMessages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


interface PersistentContext {
    fun get(conversation: Conversation): PersistentConversation
}

@AccountScope
class StatefulPersistentContext @Inject constructor(
    private val owner: User,
    private val eventPublisher: ListenableEventPublisher,
    private val onlineRepository: OnlineRepository,
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val sessionScope: CoroutineScope,
) : PersistentContext, ApplicationEventListener, AccountLifecycle {

    private val snapshotMap = mutableMapOf<Conversation, ChatSnapshot>()
    private val persistentMap = mutableMapOf<Conversation, PersistentConversation>()
    private val messageMap = mutableMapOf<Conversation, MutableStateFlow<List<Message>>>()
    private val onlineMap = mutableMapOf<Conversation, MutableStateFlow<Online>>()

    init {
        eventPublisher.register(this)
        chatRepository.observe { snapshot ->
            val persistent = get(snapshot.conversation)
            persistent append snapshot.eventList
        }
        eventRepository.observe { event ->
            val persistent = get(event.conversation)
            persistent append event
        }
        onlineRepository.observe { online ->
            val conversation = Conversation(owner, online.user)
            val onlineFlow = onlineMap.get(conversation)
            onlineFlow?.value = online
        }
    }

    override suspend fun onLogout() {
        eventPublisher.unRegister(this)
    }

    private fun persist(conversation: Conversation): PersistentConversation {
        val messageFlow = MutableStateFlow<List<Message>>(emptyList())
        val onlineFlow = MutableStateFlow(Online(0L, conversation.partner))
        val persistent = PersistentConversation(conversation, messageFlow.asStateFlow(), onlineFlow.asStateFlow())

        persistentMap.put(conversation, persistent)
        messageMap.put(conversation, messageFlow)
        onlineMap.put(conversation, onlineFlow)
        snapshotMap.put(conversation, ChatSnapshot(conversation))

        return persistent
    }

    private infix fun PersistentConversation.append(events: List<ChatEvent>) {
        val snapshot = snapshotMap.get(conversation)!!
        snapshot.addAll(events)
        emitMessages(snapshot.eventList)
    }

    private infix fun PersistentConversation.append(event: ChatEvent) {
        val snapshot = snapshotMap.get(conversation)!!
        snapshot.add(event)
        emitMessages(snapshot.eventList)
    }

    private infix fun PersistentConversation.prepend(event: ChatEvent) {
        val snapshot = snapshotMap.get(conversation)!!
        snapshot.addFirst(event)
        emitMessages(snapshot.eventList)
    }

    private fun PersistentConversation.emitMessages(eventList: List<ChatEvent>) {
        messageMap.get(conversation)!!.value = conversation.toMessages(eventList)
    }

    override fun get(conversation: Conversation): PersistentConversation =
        (persistentMap.get(conversation) ?: persist(conversation)).also {
            val partner = conversation.partner
            sessionScope.launch {
                onlineRepository.get(partner.username)
            }
        }

    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
        eventType.isAssignableFrom(ChatEvent::class.java)

    override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
        val event = applicationEvent as ChatEvent
        val persistent = get(event.conversation)
        persistent prepend event
    }
}


@Module
@InstallIn(AccountComponent::class)
abstract class PersistentContextModule {
    @Binds
    @IntoSet
    abstract fun accountLifeCycle(persistentRepository: StatefulPersistentContext): AccountLifecycle

    @Binds
    abstract fun persistentContext(persistentRepository: StatefulPersistentContext): PersistentContext
}