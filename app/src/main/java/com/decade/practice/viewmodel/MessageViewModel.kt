package com.decade.practice.viewmodel

import com.decade.practice.JobDelay
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.User
import com.decade.practice.model.domain.isMessage
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.model.presentation.Message
import com.decade.practice.model.presentation.OwnerMessage
import com.decade.practice.model.presentation.SendState
import com.decade.practice.model.presentation.toMessage
import com.decade.practice.session.repository.DialogRepository
import com.decade.practice.session.repository.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking

@HiltViewModel(assistedFactory = MessageViewModelFactory::class)
class MessageViewModel @AssistedInject constructor(
      @Assisted conversation: Conversation,
      @Assisted messageRepoFactory: MessageRepository.Factory,
      @Assisted private val dialogRepository: DialogRepository,
      jobDelay: JobDelay,
      private val formater: RollInFormater,
      private val eventPublisher: ListenableEventPublisher
) : ExpandableViewmodel<Long, Message>(jobDelay), ApplicationEventListener {
      override val listRepository = messageRepoFactory.create(conversation)

      val dialog: Dialog = runBlocking {
            dialogRepository.get(conversation)
      }
      private val conversation: Conversation
            get() = dialog.conversation
      private val chat: Chat
            get() = conversation.chat
      private val partner: User
            get() = conversation.partner
      private val owner: User
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

      init {
            itemList = dialog.messages

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
            list.forEach { message ->
                  if (message.sender == partner.id) {
                        message.seenAt = partnerSeen?.seenAt ?: Long.MIN_VALUE
                  } else {
                        if (lastSent != null) {
                              (message as OwnerMessage).isLastSent = false
                        } else {
                              if ((message as OwnerMessage).sendState == SendState.Sent) {
                                    message.isLastSent = true
                                    lastSent = message
                              }
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
                        if (lastSeen == null)
                              lastSeen = message
                        message.seenAt = at

                        if (message.seen) {
                              message.isLastSeen = false
                              return@forEach
                        }
                  }
            }
            lastSeen?.isLastSeen = true
            return lastSeen
      }

      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
            eventType.isAssignableFrom(ChatEvent::class.java)


      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            val event = applicationEvent as ChatEvent
            if (chat != event.chat)
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
            dialogRepository: DialogRepository
      ): MessageViewModel
}