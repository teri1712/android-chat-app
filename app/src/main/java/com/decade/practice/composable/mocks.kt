package com.decade.practice.composable

import com.decade.practice.R
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ChatIdentifier
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.IconEvent
import com.decade.practice.model.domain.ImageEvent
import com.decade.practice.model.domain.ImageSpec
import com.decade.practice.model.domain.TextEvent
import com.decade.practice.model.domain.User
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.model.presentation.Message
import com.decade.practice.model.presentation.OwnerMessage
import com.decade.practice.model.presentation.PartnerMessage
import com.decade.practice.utils.ONE_MINUTE_SECONDS
import com.decade.practice.utils.ONE_WEEK_MILLIS
import java.time.Instant
import java.util.UUID

val mockUser = User(
      UUID.nameUUIDFromBytes("666".toByteArray()).toString(),
      "Luffy",
      "Monkey D Luffy",
      "Male",
      ImageSpec("", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRPqn67k230n8waDkEoB51rcqoSR6aCf-maRg&s", 100, 100),
      "User"
)

fun mockPartner() = User(
      UUID.randomUUID().toString(),
      "Nami",
      "Nami",
      "Female",
      ImageSpec("", "https://i.pinimg.com/736x/f9/ad/59/f9ad59d7c345d7066593d1471999d18d.jpg", 100, 100),
      "ROLE_USER"
)

fun mockConversation(): Conversation {
      val partner = mockPartner()
      return Conversation(
            Chat(ChatIdentifier.from(mockUser, partner), mockUser.id),
            partner,
            mockUser,
      )
}

fun mockDialog(): Dialog {
      val conversation = mockConversation()
      return object : Dialog {
            override val conversation: Conversation
                  get() = conversation
            override val onlineAt: Long
                  get() = Instant.now().epochSecond - (0..1).random() * ONE_MINUTE_SECONDS
            override val newest: Message
                  get() = mockTextMessage(conversation).also {
                        it.seenAt = System.currentTimeMillis()
                  }
            override val messages: List<Message>
                  get() = emptyList()
      }
}

fun mockTextMessage(conversation: Conversation, mine: Boolean = false): Message {
      val textEvent = TextEvent("Hello")
      val chatEvent =
            ChatEvent(
                  conversation.identifier,
                  if (mine) conversation.owner.id
                  else conversation.partner.id,
                  textEvent = textEvent,
                  receiveTime = System.currentTimeMillis() - ONE_WEEK_MILLIS + 2 * 60 * 1000
            )
      return if (mine) OwnerMessage(chatEvent) else PartnerMessage(chatEvent)
}

fun mockIconMessage(conversation: Conversation, mine: Boolean = false): Message {
      val iconEvent = IconEvent(R.drawable.blue_like_button_icon)
      val chatEvent =
            ChatEvent(
                  conversation.identifier,
                  if (mine) conversation.owner.id
                  else conversation.partner.id,
                  iconEvent = iconEvent,
                  receiveTime = System.currentTimeMillis() - ONE_WEEK_MILLIS + 2 * 60 * 1000
            )
      return if (mine) OwnerMessage(chatEvent) else PartnerMessage(chatEvent)
}

fun mockImageMessage(conversation: Conversation, mine: Boolean = false): Message {
      val imageEvent = ImageEvent(ImageSpec("", "https://i.pinimg.com/736x/92/90/4f/92904fb81b692e508efa3dfe55190829.jpg", 500, 500))
      val chatEvent =
            ChatEvent(
                  conversation.identifier,
                  if (mine) conversation.owner.id
                  else conversation.partner.id,
                  imageEvent = imageEvent,
                  receiveTime = System.currentTimeMillis() - ONE_WEEK_MILLIS + 2 * 60 * 1000
            )
      return if (mine) OwnerMessage(chatEvent) else PartnerMessage(chatEvent)
}
