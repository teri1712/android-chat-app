package com.decade.practice.components

import com.decade.practice.message.ListenableMessageQueue
import com.decade.practice.message.MessageQueue
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton


const val INBOUND_CHANNEL = "INBOUND_QUEUE"
const val OUTBOUND_CHANNEL = "OUTBOUND_QUEUE"

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagingModule {

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

