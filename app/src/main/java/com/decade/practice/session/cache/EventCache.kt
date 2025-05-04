package com.decade.practice.session.cache

import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ChatSnapshot
import com.decade.practice.model.domain.Conversation
import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import javax.inject.Inject

@AccountScope
class EventCache @Inject constructor(private val eventPublisher: ListenableEventPublisher) :
      Cache<Conversation, List<ChatEvent>>,
      ApplicationEventListener,
      AccountLifecycle {

      override suspend fun onStart() {
            eventPublisher.register(this)
      }

      override suspend fun onLogout() {
            eventPublisher.unRegister(this)
      }

      private val snapshotMap = HashMap<Conversation, ChatSnapshot>()

      override fun save(conversation: Conversation, eventList: List<ChatEvent>) =
            getSnapshot(conversation).addAll(eventList)

      private fun getSnapshot(conversation: Conversation): ChatSnapshot =
            snapshotMap.getOrPut(conversation) {
                  ChatSnapshot(conversation)
            }

      override fun get(conversation: Conversation): List<ChatEvent> =
            getSnapshot(conversation).eventList


      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
            eventType.isAssignableFrom(ChatEvent::class.java)

      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            val event = applicationEvent as ChatEvent
            val snapshot = getSnapshot(event.conversation)
            snapshot.addFirst(event)
      }

      @dagger.Module
      @InstallIn(AccountComponent::class)
      abstract class Module {
            @Binds
            @IntoSet
            abstract fun accountLifeCycle(eventCache: EventCache): AccountLifecycle
      }
}


