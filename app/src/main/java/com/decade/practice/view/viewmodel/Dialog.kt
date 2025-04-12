package com.decade.practice.view.viewmodel

import androidx.compose.runtime.Stable
import com.decade.practice.model.Conversation

@Stable
interface Dialog {
    val conversation: Conversation
    val onlineAt: Long
    val newest: Message

    fun goOnline()
    fun goOffline()
}
