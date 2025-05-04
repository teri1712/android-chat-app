package com.decade.practice.viewmodel

import com.decade.practice.JobDelay
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.isMessage
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.session.repository.DialogRepository
import com.decade.practice.utils.union
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = ThreadViewModelFactory::class)
class ThreadViewModel @AssistedInject constructor(
      @Assisted override val listRepository: DialogRepository,
      private val eventPublisher: ListenableEventPublisher,
      jobDelay: JobDelay
) : ExpandableViewmodel<Conversation, Dialog>(jobDelay), ApplicationEventListener {

      private val _conversationFlow = MutableStateFlow(emptyList<Dialog>())
      val conversationFlow = _conversationFlow.asStateFlow()
      private var conversationList = emptyList<Dialog>()
            set(value) {
                  field = value
                  _conversationFlow.value = value
            }

      init {
            eventPublisher.register(this)
            expand()
      }

      override fun onCleared() {
            eventPublisher.unRegister(this)
            super.onCleared()
      }

      override fun append(list: List<Dialog>) {
            conversationList = conversationList union list
            itemList = itemList union list
      }

      private fun prepend(conversation: Dialog, eventCommited: Boolean) {
            conversationList = listOf(conversation) + conversationList
            if (eventCommited) {
                  itemList = listOf(conversation) + itemList
            }
      }

      private fun remove(conversation: Dialog, eventCommited: Boolean) {
            if (conversationList.contains(conversation)) {
                  conversationList -= conversation
            }
            if (eventCommited) {
                  if (itemList.contains(conversation)) {
                        itemList -= conversation
                  }
            }
      }

      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
            eventType.isAssignableFrom(ChatEvent::class.java)


      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            val event = applicationEvent as ChatEvent
            if (event.isMessage()) {
                  val conversation = event.conversation
                  val dialog = listRepository.get(conversation)
                  remove(dialog, event.committed)
                  prepend(dialog, event.committed)
            }
      }
}


@AssistedFactory
interface ThreadViewModelFactory {
      fun create(dialogRepository: DialogRepository): ThreadViewModel
}


