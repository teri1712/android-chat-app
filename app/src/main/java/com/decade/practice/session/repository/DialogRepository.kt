package com.decade.practice.session.repository

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ChatIdentifier
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.Online
import com.decade.practice.model.domain.User
import com.decade.practice.model.domain.isMessage
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.model.presentation.Message
import com.decade.practice.model.presentation.toMessage
import com.decade.practice.model.presentation.toMessages
import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import com.decade.practice.session.cache.EventCache
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
class PersistentDialog(
      override val conversation: Conversation,
      private val eventCache: EventCache
) : Dialog {
      override var newest: Message by mutableStateOf(Message)
      override var onlineAt: Long by mutableStateOf(Long.MIN_VALUE)
      override val messages: List<Message>
            get() = conversation.toMessages(eventCache.get(conversation))
}


@AccountScope
class DialogRepository @Inject constructor(
      private val account: User,
      private val chatRepo: ChatRepository,
      private val coroutine: CoroutineScope,
      private val eventCache: EventCache,
      private val onlineRepo: OnlineRepository,
      private val eventPublisher: ListenableEventPublisher
) : Repository<Dialog, Conversation>, ApplicationEventListener, AccountLifecycle {

      private val dialogMap = HashMap<ChatIdentifier, PersistentDialog>()

      override suspend fun onStart() {
            eventPublisher.register(this)
      }

      override suspend fun onLogout() {
            eventPublisher.unRegister(this)
      }

      override suspend fun list(index: Conversation?): List<Dialog> =
            chatRepo.list(index).map { snapshot ->
                  val conversation = snapshot.conversation
                  val dialog = get(conversation)
                  eventCache.save(conversation, snapshot.eventList)
                  dialog.newest = conversation.toMessages(eventCache.get(conversation)).first()
                  dialog
            }

      override suspend fun get(index: Conversation): PersistentDialog {
            var dialog = dialogMap.get(index.identifier)
            if (dialog == null) {
                  dialog = PersistentDialog(index, eventCache)
                  dialogMap.put(index.identifier, dialog)
            }
            return dialog
      }


      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
            eventType.isAssignableFrom(ChatEvent::class.java) || eventType.isAssignableFrom(Online::class.java)

      override suspend fun onApplicationEvent(event: ApplicationEvent) {
            if (event is ChatEvent) {

                  val conversation = event.conversation
                  val persistent = get(event.conversation)
                  if (event.isMessage()) {
                        persistent.newest = conversation.toMessage(event)
                  } else if (
                        persistent.newest !== Message &&
                        persistent.newest.sender == event.sender
                  ) {
                        persistent.newest.seenAt = event.seenEvent!!.at
                  }
                  coroutine.launch {
                        onlineRepo.get(conversation.partner.username)
                  }
            } else if (event is Online) {
                  val persistent = dialogMap.get(ChatIdentifier.from(account, event.user))
                  if (persistent != null)
                        persistent.onlineAt = event.at
            }
      }


      @dagger.Module
      @InstallIn(AccountComponent::class)
      abstract class Module {
            @Binds
            @IntoSet
            abstract fun accountLifeCycle(dialogRepository: DialogRepository): AccountLifecycle
      }
}


