package com.decade.practice.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.decade.practice.utils.formatRelativeTime
import com.decade.practice.view.viewmodel.ListIndex
import java.util.LinkedList

@Stable
abstract class Message(
    id: String,
    sender: String,
    textEvent: TextEvent? = null,
    iconEvent: IconEvent? = null,
    imageEvent: ImageEvent? = null,
    receiveTime: Long,
) : ListIndex<Long> {

    constructor(chatEvent: ChatEvent) : this(
        chatEvent.id,
        chatEvent.sender,
        chatEvent.textEvent,
        chatEvent.iconEvent,
        chatEvent.imageEvent,
        chatEvent.receiveTime,
    )

    val id: String = id
    val sender: String = sender
    val textEvent: TextEvent? = textEvent
    val imageEvent: ImageEvent? = imageEvent
    val iconEvent: IconEvent? = iconEvent
    var seenAt by mutableLongStateOf(Long.MIN_VALUE)
    var receiveTime by mutableLongStateOf(receiveTime)
    var position by mutableStateOf(Position.Single)
    var fixedDisplayTime by mutableStateOf(false)
    var isLastSeen by mutableStateOf(false)

    val seen: Boolean
        get() = Long.MIN_VALUE != seenAt

    override val index: Long
        get() = receiveTime

    companion object : Message("", "", null, null, null, 0L)
}

class PartnerMessage(chatEvent: ChatEvent) : Message(chatEvent)

class OwnerMessage(chatEvent: ChatEvent) : Message(chatEvent) {
    var sendState by mutableStateOf(SendState.Pending)
    var isLastSent by mutableStateOf(false)

    init {
        sendState = when {
            chatEvent.eventVersion != null -> SendState.Sent
            chatEvent.committed -> SendState.Sending
            else -> SendState.Pending
        }
    }
}

enum class Position {
    Top, Center, Bottom, Single
}

enum class SendState {
    Pending, Sending, Sent;

    override fun toString(): String {
        return when (this) {
            Pending -> ""
            Sending -> "Pending"
            Sent -> "Sent"
        }
    }
}

fun Conversation.toMessage(event: ChatEvent): Message {
    assert(event.isMessage())
    return if (event.sender == owner.id) OwnerMessage(event) else PartnerMessage(event)
}

fun Conversation.toMessages(events: List<ChatEvent>): List<Message> {
    val messages = LinkedList<Message>()
    var partnerSeen: SeenEvent? = null
    var ownerSeen: SeenEvent? = null
    var lastSent: OwnerMessage? = null
    for (event in events) {
        if (event.seenEvent != null) {
            if (partner.id == event.sender) {
                partnerSeen = event.seenEvent
            } else {
                ownerSeen = event.seenEvent
            }
        } else {
            var message: Message
            if (owner.id == event.sender) {
                message = OwnerMessage(event)
                if (partnerSeen != null)
                    message.seenAt = partnerSeen.at
                if (event.eventVersion != null && lastSent == null) {
                    lastSent = message
                    lastSent.isLastSent = true
                }
            } else {
                message = PartnerMessage(event)
                if (ownerSeen != null)
                    message.seenAt = ownerSeen.at
            }
            messages.add(message)
        }
    }
    return messages
}

fun Conversation.announcementOf(newest: Message): String {
    val mine = newest.sender == owner.id
    val content = when {
        newest.imageEvent != null -> "has sent an image"
        newest.iconEvent != null -> "has sent an icon"
        newest.textEvent != null -> newest.textEvent.content
        else -> ""
    } + " · " + formatRelativeTime(newest.receiveTime)
    val sub = when {
        mine -> "You"
        else -> partner.name
    }
    val sep = when {
        newest.textEvent != null -> " : "
        else -> " "
    }
    return sub + sep + content
}