package com.decade.practice.model.domain

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.decade.practice.event.ApplicationEvent
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.UUID


const val TEXT: String = "TEXT"
const val IMAGE: String = "IMAGE"
const val ICON: String = "ICON"
const val SEEN: String = "SEEN"
const val WELCOME: String = "WELCOME"


@Entity(
      foreignKeys = [
            ForeignKey(
                  entity = Chat::class,
                  parentColumns = ["firstUser", "secondUser"],
                  childColumns = ["firstUser", "secondUser"],
                  onDelete = ForeignKey.CASCADE
            ),
      ], indices = [Index(value = ["eventVersion"]),
            Index(value = ["receiveTime"]),
            Index(value = ["eventType"])]
)
data class ChatEvent(
      @Embedded
      val chatIdentifier: ChatIdentifier,
      val sender: String,

      @Embedded(prefix = "text_event_")
      val textEvent: TextEvent? = null,

      @Embedded(prefix = "seen_event_")
      val seenEvent: SeenEvent? = null,

      @Embedded(prefix = "image_event_")
      val imageEvent: ImageEvent? = null,

      @Embedded(prefix = "icon_event_")
      val iconEvent: IconEvent? = null,

      @PrimaryKey
      val id: String = UUID.randomUUID().toString(),

      val createdTime: Long = System.currentTimeMillis(),
      var receiveTime: Long = System.currentTimeMillis(),

      val eventType: String = when {
            textEvent != null -> TEXT
            imageEvent != null -> IMAGE
            iconEvent != null -> ICON
            seenEvent != null -> SEEN
            else -> ""
      }
) : ApplicationEvent() {

      var committed: Boolean = false
      var eventVersion: Int? = null

      val sending: Boolean
            get() = eventVersion == null


      @Expose(serialize = false)
      @Ignore
      lateinit var partner: User

      @Expose(serialize = false)
      @Ignore
      lateinit var owner: User

      @Expose(serialize = false)
      @Ignore
      lateinit var chat: Chat

      var conversation: Conversation
            get() = Conversation(chat, partner, owner)
            set(value) {
                  owner = value.owner
                  partner = value.partner
                  chat = Chat(value.identifier, owner)
            }

      @SerializedName("content")
      @Expose(deserialize = false)
      @Ignore
      private val json_extra_content = textEvent?.content

      @SerializedName("image")
      @Expose(deserialize = false)
      @Ignore
      private val json_extra_image = imageEvent?.imageSpec

      @SerializedName("at")
      @Expose(deserialize = false)
      @Ignore
      private val json_extra_at = seenEvent?.at

      @SerializedName("resourceId")
      @Expose(deserialize = false)
      @Ignore
      private val json_extra_resourceId = iconEvent?.resourceId

      override fun equals(other: Any?): Boolean {
            if (other !is ChatEvent)
                  return false
            return other.id == id
      }

      override fun hashCode(): Int {
            return id.hashCode()
      }
}


@Immutable
data class TextEvent(val content: String) {
}

@Immutable
data class SeenEvent(val at: Long) {
}

@Immutable
data class ImageEvent(@Embedded val imageSpec: ImageSpec) {
      val uri: String
            get() = imageSpec.uri
      val width: Int
            get() = imageSpec.width
      val height: Int
            get() = imageSpec.height

}

@Immutable
data class IconEvent(val resourceId: Int) {

}

fun placeHolder(width: Int, height: Int): Bitmap {
      val placeHolder = createBitmap(width, height)
      placeHolder.eraseColor(Color(0, 0, 0, 30).toArgb())
      return placeHolder
}


fun ChatEvent.isMessage() = textEvent != null || imageEvent != null || iconEvent != null

