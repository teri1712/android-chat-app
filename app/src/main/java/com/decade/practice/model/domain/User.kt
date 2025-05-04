package com.decade.practice.model.domain

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
@Immutable

data class User(
    @PrimaryKey
    val id: String,
    val username: String = "",
    val name: String = "",
    val gender: String = "",
    @Embedded
    val avatar: ImageSpec,
    val role: String = ""
) {

    override fun equals(other: Any?): Boolean {
        if (other !is User) return false
        return id == other.id
    }


    override fun hashCode(): Int = id.hashCode()
}
