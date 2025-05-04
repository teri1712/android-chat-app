package com.decade.practice

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.decade.practice.message.MessageChannel
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.IconEvent
import com.decade.practice.model.domain.ImageEvent
import com.decade.practice.model.domain.ImageSpec
import com.decade.practice.model.domain.SeenEvent
import com.decade.practice.model.domain.TextEvent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Immutable
@Singleton
class MessageService @Inject constructor(
      private val context: Context,
      @Named(OUTBOUND_CHANNEL)
      private val channel: MessageChannel
) {

      fun seen(conversation: Conversation) {
            val event = ChatEvent(
                  chatIdentifier = conversation.identifier,
                  sender = conversation.owner.id,
                  seenEvent = SeenEvent(System.currentTimeMillis())
            )
            event.conversation = conversation
            channel.enqueue(event)
      }

      fun send(conversation: Conversation, text: String) {
            val event = ChatEvent(
                  chatIdentifier = conversation.identifier,
                  sender = conversation.owner.id,
                  textEvent = TextEvent(text)
            )
            event.conversation = conversation
            channel.enqueue(event)
      }

      fun send(conversation: Conversation, resourceId: Int) {
            val event = ChatEvent(
                  chatIdentifier = conversation.identifier,
                  sender = conversation.owner.id,
                  iconEvent = IconEvent(resourceId)
            )
            event.conversation = conversation
            channel.enqueue(event)
      }

      fun send(conversation: Conversation, uri: Uri) {
            //TODO: width and height only
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            val event = ChatEvent(
                  chatIdentifier = conversation.identifier,
                  sender = conversation.owner.id,
                  imageEvent = ImageEvent(ImageSpec("", uri.toString(), bitmap.width, bitmap.height))
            )
            event.conversation = conversation
            channel.enqueue(event)
      }
}
