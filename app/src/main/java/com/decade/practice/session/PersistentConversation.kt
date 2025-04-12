package com.decade.practice.session


import com.decade.practice.model.Conversation
import com.decade.practice.model.Online
import com.decade.practice.view.viewmodel.Message
import kotlinx.coroutines.flow.StateFlow


class PersistentConversation(
    val conversation: Conversation,
    val messageFlow: StateFlow<List<Message>>,
    val onlineFlow: StateFlow<Online>
) {
    val messages: List<Message>
        get() = messageFlow.value
}
