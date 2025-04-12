package com.decade.practice.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Account(

    @PrimaryKey
    val id: String,

    val username: String,

    @Embedded(prefix = "user_")
    val user: User,

    @Embedded
    val credential: Credential,

    @Embedded
    val syncContext: SyncContext,
) {

    val accessToken: String
        get() = credential.accessToken


    override fun equals(other: Any?): Boolean {
        if (other !is Account)
            return false
        return other.id == id
    }
}
