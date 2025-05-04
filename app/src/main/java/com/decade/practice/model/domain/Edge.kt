package com.decade.practice.model.domain

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["firstUser", "secondUser"],
            childColumns = ["fromfirstUser", "fromsecondUser"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["fromfirstUser", "fromsecondUser"]
)
data class RemoteEdge(

    @Embedded(prefix = "from")
    val from: ChatIdentifier,

    @Embedded(prefix = "to")
    val to: ChatIdentifier,
) {
    constructor(from: Chat, to: Chat) : this(from.identifier, to.identifier)
}


@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["firstUser", "secondUser"],
            childColumns = ["fromfirstUser", "fromsecondUser"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["fromfirstUser", "fromsecondUser"]
)
data class LocalEdge(

    @Embedded(prefix = "from")
    val from: ChatIdentifier,

    @Embedded(prefix = "to")
    val to: ChatIdentifier,
) {

    constructor(from: Chat, to: Chat) : this(from.identifier, to.identifier)
}