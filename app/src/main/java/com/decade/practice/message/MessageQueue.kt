package com.decade.practice.message

import com.decade.practice.model.ChatEvent

interface MessageQueue {
    fun enqueue(event: ChatEvent)
}

interface QueueListener {
    fun onMessage(chatEvent: ChatEvent)
}

class ListenableMessageQueue : MessageQueue {
    private val listeners = HashSet<QueueListener>()
    fun register(listener: QueueListener) {
        listeners.add(listener)
    }

    fun unRegister(listener: QueueListener) {
        listeners.remove(listener)
    }

    override fun enqueue(event: ChatEvent) {
        listeners.forEach {
            it.onMessage(event)
        }
    }
}
