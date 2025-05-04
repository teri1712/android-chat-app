package com.decade.practice.model.domain

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Conversation(
      @Embedded
      val chat: Chat,

      @Relation(parentColumn = "partner", entityColumn = "id")
      val partner: User,

      @Relation(parentColumn = "owner", entityColumn = "id")
      val owner: User
) {


      constructor(owner: User, partner: User) : this(Chat(ChatIdentifier.from(owner, partner), owner), partner, owner)

      val identifier
            get() = chat.identifier

      override fun hashCode(): Int = identifier.hashCode()

      override fun equals(other: Any?): Boolean {
            if (other !is Conversation)
                  return false
            return other === this || other.identifier == identifier
      }


}