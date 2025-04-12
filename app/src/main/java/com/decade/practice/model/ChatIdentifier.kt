package com.decade.practice.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.Objects
import java.util.UUID

@Serializable
@Immutable
data class ChatIdentifier(
    val firstUser: String,
    val secondUser: String
) {

    override fun equals(other: Any?): Boolean {
        if (other !is ChatIdentifier)
            return false
        return other.firstUser == firstUser && other.secondUser == secondUser
    }

    override fun hashCode(): Int {
        return Objects.hash(firstUser, secondUser)
    }

    override fun toString(): String {
        return "$firstUser+$secondUser"
    }

    companion object {
        fun from(first: User, second: User): ChatIdentifier = from(first.id, second.id)

        fun from(first: String, second: String): ChatIdentifier =
            if (UUID.fromString(first) < UUID.fromString(second)) ChatIdentifier(first, second)
            else ChatIdentifier(second, first)
    }
}

fun ChatIdentifier.inspectPartner(account: String): String {
    return if (firstUser == account) secondUser else firstUser
}

fun ChatIdentifier.inspectPartner(account: User): String {
    return inspectPartner(account.id)
}

