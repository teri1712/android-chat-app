package com.decade.practice.model.presentation

import androidx.compose.runtime.Stable
import com.decade.practice.model.domain.Conversation

@Stable
interface Dialog : ListIndex<Conversation> {
      val conversation: Conversation
      val onlineAt: Long
      val newest: Message
      val messages: List<Message>

      override val index: Conversation
            get() = conversation

}
