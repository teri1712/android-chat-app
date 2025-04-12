package com.decade.practice.components

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.decade.practice.session.AccountScope
import com.decade.practice.message.MessageQueue
import com.decade.practice.message.OUTBOUND_CHANNEL
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.Conversation
import com.decade.practice.model.IconEvent
import com.decade.practice.model.ImageEvent
import com.decade.practice.model.ImageSpec
import com.decade.practice.model.SeenEvent
import com.decade.practice.model.TextEvent
import javax.inject.Inject
import javax.inject.Named


@AccountScope
@Immutable
class MessageService @Inject constructor(
    private val context: Context,
    @Named(OUTBOUND_CHANNEL) private val queue: MessageQueue
) {

    fun seen(conversation: Conversation) {
        val event = ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.owner.id,
            seenEvent = SeenEvent(System.currentTimeMillis())
        )
        event.conversation = conversation
        queue.enqueue(event)
    }

    fun send(conversation: Conversation, text: String) {
        val event = ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.owner.id,
            textEvent = TextEvent(text)
        )
        event.conversation = conversation
        queue.enqueue(event)
    }

    fun send(conversation: Conversation, resourceId: Int) {
        val event = ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.owner.id,
            iconEvent = IconEvent(resourceId)
        )
        event.conversation = conversation
        queue.enqueue(event)
    }

    fun send(conversation: Conversation, uri: Uri) {
        //TODO: width and height only
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)

        val event = ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.owner.id,
            imageEvent = ImageEvent(ImageSpec(uri.toString(), bitmap.width, bitmap.height))
        )
        event.conversation = conversation
        queue.enqueue(event)
    }
}
