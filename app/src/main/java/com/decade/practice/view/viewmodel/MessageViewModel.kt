package com.decade.practice.view.viewmodel

import com.decade.practice.components.JobDelay
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.Conversation
import com.decade.practice.model.Message
import com.decade.practice.model.Online
import com.decade.practice.model.OwnerMessage
import com.decade.practice.model.SendState
import com.decade.practice.model.User
import com.decade.practice.model.isMessage
import com.decade.practice.model.toMessage
import com.decade.practice.repository.MessageRepository
import com.decade.practice.session.PersistentContext
import com.decade.practice.utils.RollInFormater
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel(assistedFactory = MessageViewModelFactory::class)
class MessageViewModel @AssistedInject constructor(
    @Assisted val conversation: Conversation,
    @Assisted messageRepoFactory: MessageRepository.Factory,
    @Assisted private val persistentContext: PersistentContext,
    jobDelay: JobDelay,
    private val formater: RollInFormater,
    private val eventPublisher: ListenableEventPublisher
) : ExpandableViewmodel<Long, Message>(jobDelay), ApplicationEventListener {

    override val repository = messageRepoFactory.create(conversation)

    val partner: User
        get() = conversation.partner
    val owner: User
        get() = conversation.owner

    var partnerSeen: Message? = null
        private set
    var ownerSeen: Message? = null
        private set
    var lastSent: OwnerMessage? = null
        private set

    override var itemList: List<Message>
        get() = super.itemList
        set(value) {
            super.itemList = value
            reformat()
        }

    var onlineFlow: StateFlow<Online>
        private set

    init {
        val persistent = persistentContext.get(conversation)
        itemList = persistent.messages

        partnerSeen = itemList.firstOrNull { message ->
            message.sender == owner.id && message.seen
        }?.apply {
            isLastSeen = true
        }

        ownerSeen = itemList.firstOrNull { message ->
            message.sender == partner.id && message.seen
        }?.apply {
            isLastSeen = true
        }

        lastSent = itemList.firstOrNull { message ->
            message.sender == owner.id && (message as OwnerMessage).sendState == SendState.Sent
        }?.apply {
            (this as OwnerMessage).isLastSent = true
        } as OwnerMessage?

        onlineFlow = persistent.onlineFlow

        formater.format(itemList)
        eventPublisher.register(this)
        if (itemList.size < 20)
            expand()
    }

    override fun onCleared() {
        eventPublisher.unRegister(this)
        super.onCleared()
    }

    override fun append(list: List<Message>) {
        itemList.forEach { message ->
            if (message.sender == partner.id) {
                message.seenAt = partnerSeen?.seenAt ?: Long.MIN_VALUE
            } else {
                if (lastSent == null && (message as OwnerMessage).sendState == SendState.Sent) {
                    lastSent = message
                    lastSent?.isLastSent = true
                }
                message.seenAt = ownerSeen?.seenAt ?: Long.MIN_VALUE
            }
        }

        super.append(list)
    }

    private fun reformat() {
        formater.reformat(itemList)
    }

    private fun prepend(event: ChatEvent) {
        if (event.committed) {
            itemList.forEach { message ->
                if (message.id == event.id) {
                    message.receiveTime = event.receiveTime
                    if (message is OwnerMessage) {
                        message.sendState = when {
                            event.eventVersion != null -> SendState.Sent
                            event.committed -> SendState.Sending
                            else -> SendState.Pending
                        }
                        if (event.eventVersion != null) {
                            lastSent?.isLastSent = false
                            lastSent = message
                            lastSent?.isLastSent = true
                        }
                    }
                    return
                }
            }
        }
        val message = conversation.toMessage(event)
        itemList = listOf(message) + itemList
    }

    private fun updateSeen(who: String, at: Long): Message? {
        var lastSeen: Message? = null
        itemList.forEach { message: Message ->
            if (message.sender != who) {
                if (message.seenAt != Long.MIN_VALUE) {
                    message.isLastSeen = false
                    return@forEach
                }
                if (lastSeen == null) {
                    lastSeen = message
                }
                message.seenAt = at
            }
        }
        lastSeen?.isLastSeen = true
        return lastSeen
    }

    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
        eventType.isAssignableFrom(ChatEvent::class.java)


    override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
        val event = applicationEvent as ChatEvent
        if (conversation.chat != event.chat)
            return
        if (event.isMessage()) {
            prepend(event)
        } else {
            val seenAt = event.seenEvent!!.at
            if (event.sender == owner.id)
                ownerSeen = updateSeen(owner.id, seenAt)
            else
                partnerSeen = updateSeen(partner.id, seenAt)
        }
    }

}


@AssistedFactory
interface MessageViewModelFactory {
    fun create(
        conversation: Conversation,
        messageRepoFactory: MessageRepository.Factory,
        persistentContext: PersistentContext
    ): MessageViewModel
}