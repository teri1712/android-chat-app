package com.decade.practice.view.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.decade.practice.components.JobDelay
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.event.ListenableEventPublisher
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.Conversation
import com.decade.practice.model.isMessage
import com.decade.practice.repository.ConversationRepository
import com.decade.practice.session.PersistentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ThreadViewModelFactory::class)
class ThreadViewModel @AssistedInject constructor(
    @Assisted override val repository: ConversationRepository,
    @Assisted private val persistentContext: PersistentContext,
    private val eventPublisher: ListenableEventPublisher,
    jobDelay: JobDelay
) : ExpandableViewmodel<Conversation, Conversation>(jobDelay), ApplicationEventListener {

    private val _conversationFlow = MutableStateFlow(emptyList<ThreadDialog>())
    val conversationFlow = _conversationFlow.asStateFlow()

    private var actualList = emptyList<Conversation>()

    override var itemList: List<Conversation>
        get() = super.itemList
        set(value) {
            super.itemList = value
            _conversationFlow.value = value.map { ThreadDialog(it) }
        }

    init {
        eventPublisher.register(this)
        expand()
    }

    override fun onCleared() {
        eventPublisher.unRegister(this)
        super.onCleared()
    }

    override fun append(list: List<Conversation>) {
        actualList = actualList union list
        itemList = itemList union list
    }

    private fun prepend(conversation: Conversation, eventCommited: Boolean) {
        itemList = listOf(conversation) + itemList
        if (eventCommited) {
            actualList = listOf(conversation) + actualList
        }
    }

    private fun remove(conversation: Conversation, eventCommited: Boolean) {
        if (itemList.contains(conversation)) {
            itemList -= conversation
        }

        if (eventCommited) {
            actualList -= conversation
        }
    }

    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
        eventType.isAssignableFrom(ChatEvent::class.java)


    override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
        val event = applicationEvent as ChatEvent
        if (event.isMessage()) {
            val conversation = event.conversation
            remove(conversation, event.committed)
            prepend(conversation, event.committed)
        }
    }

    @Stable
    inner class ThreadDialog(override val conversation: Conversation) : Dialog {
        private var _newest: Message by mutableStateOf(Message)
        private var _onlineAt: Long by mutableLongStateOf(0)
        private var messageJob: Job? = null
        private var onlineJob: Job? = null

        override val onlineAt: Long
            get() = _onlineAt

        override val newest: Message
            get() = _newest

        override fun goOnline() {
            val persistent = persistentContext.get(conversation)
            messageJob = viewModelScope.launch {
                persistent.messageFlow.map {
                    it.firstOrNull() ?: Message
                }.collect {
                    _newest = it
                }
            }
            onlineJob = viewModelScope.launch {
                persistent.onlineFlow.collect {
                    _onlineAt = it.at
                }
            }
        }

        override fun goOffline() {
            messageJob?.cancel()
            onlineJob?.cancel()
        }

    }
}


infix fun List<Conversation>.union(other: List<Conversation>): List<Conversation> {
    return plus(other.filter { !contains(it) })
}


@AssistedFactory
interface ThreadViewModelFactory {
    fun create(chatRepository: ConversationRepository, persistentRepo: PersistentContext): ThreadViewModel
}


