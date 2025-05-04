package com.decade.practice

import com.decade.practice.message.ListenableMessageChannel
import com.decade.practice.message.MessageChannel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton


const val INBOUND_CHANNEL = "INBOUND_CHANNEL"
const val OUTBOUND_CHANNEL = "OUTBOUND_CHANNEL"

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagingModule {

      @Binds
      @Named(INBOUND_CHANNEL)
      abstract fun inboundChannel(
            @Named(INBOUND_CHANNEL) channel: ListenableMessageChannel
      ): MessageChannel

      @Binds
      @Named(OUTBOUND_CHANNEL)
      abstract fun outboundChannel(
            @Named(OUTBOUND_CHANNEL) channel: ListenableMessageChannel
      ): MessageChannel

      companion object {
            @Provides
            @Named(INBOUND_CHANNEL)
            @Singleton
            fun inboundChannel() = ListenableMessageChannel()

            @Provides
            @Named(OUTBOUND_CHANNEL)
            @Singleton
            fun outboundChannel() = ListenableMessageChannel()
      }
}

