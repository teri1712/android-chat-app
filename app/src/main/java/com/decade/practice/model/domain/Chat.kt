package com.decade.practice.model.domain

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(
    primaryKeys = ["firstUser", "secondUser"],
)
@Serializable
@Immutable

data class Chat(
    @Embedded
    val identifier: ChatIdentifier,
    val owner: String,
    val partner: String = if (identifier.firstUser == owner)
        identifier.secondUser else identifier.firstUser
) {
    constructor(identifier: ChatIdentifier, owner: User) : this(identifier, owner.id)
}
