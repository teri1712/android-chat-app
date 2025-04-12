package com.decade.practice.net.api

import com.decade.practice.model.ChatIdentifier
import com.decade.practice.model.ChatSnapshot
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface ChatCall {

    @GET("/chat")
    suspend fun list(
        @Query("atVersion") atVersion: Int,
        @Query("startAt") startAt: ChatIdentifier,
    ): List<ChatSnapshot>

    @GET("/chat")
    suspend fun get(
        @Query("identifier") identifier: ChatIdentifier,
        @Query("atVersion") atVersion: Int? = null,
    ): ChatSnapshot

}

fun Retrofit.chatCall(): ChatCall = this.create(ChatCall::class.java)