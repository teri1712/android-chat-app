package com.decade.practice.message

import com.decade.practice.model.ChatEvent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

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

const val INBOUND_CHANNEL = "INBOUND_QUEUE"
const val OUTBOUND_CHANNEL = "OUTBOUND_QUEUE"

@Module
@InstallIn(SingletonComponent::class)
abstract class MessageChannelModule {

    @Binds
    @Named(INBOUND_CHANNEL)
    abstract fun inboundQueue(
        @Named(INBOUND_CHANNEL) queue: ListenableMessageQueue
    ): MessageQueue

    @Binds
    @Named(OUTBOUND_CHANNEL)
    abstract fun outboundQueue(
        @Named(OUTBOUND_CHANNEL) queue: ListenableMessageQueue
    ): MessageQueue

    companion object {
        @Provides
        @Named(INBOUND_CHANNEL)
        @Singleton
        fun inboundQueue() = ListenableMessageQueue()

        @Provides
        @Named(OUTBOUND_CHANNEL)
        @Singleton
        fun outboundQueue() = ListenableMessageQueue()
    }
}

