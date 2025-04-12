package com.decade.practice.model

import androidx.compose.runtime.Stable

@Stable
interface Dialog {
    val conversation: Conversation
    val onlineAt: Long
    val newest: Message

    fun goOnline()
    fun goOffline()
}
