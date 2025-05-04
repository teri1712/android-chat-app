package com.decade.practice.model.domain

data class Credential(
    val accessToken: String,
    val refreshToken: String,
    @Volatile var expiresIn: Long,
    val createdAt: Long,
) {

    val expiresAt: Long
        get() = createdAt + expiresIn

    val isExpired: Boolean
        get() = expiresAt < System.currentTimeMillis()

}
