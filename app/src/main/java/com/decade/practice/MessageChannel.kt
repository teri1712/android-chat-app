package com.decade.practice

import com.decade.practice.model.domain.ChatEvent

interface MessageChannel {
      fun enqueue(event: ChatEvent)
}

interface ChannelListener {
      fun onMessage(chatEvent: ChatEvent)
}

class ListenableMessageChannel : MessageChannel {
      private val listeners = HashSet<ChannelListener>()
      fun register(listener: ChannelListener) {
            listeners.add(listener)
      }

      fun unRegister(listener: ChannelListener) {
            listeners.remove(listener)
      }

      override fun enqueue(event: ChatEvent) {
            listeners.forEach {
                  it.onMessage(event)
            }
      }
}
