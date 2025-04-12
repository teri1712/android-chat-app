package com.decade.practice.net.api

import com.decade.practice.model.ChatEvent
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface EventCall {

    @GET("/message/chat")
    suspend fun list(
        @Query("identifier") identifier: String,
        @Query("atVersion") atVersion: Int
    ): List<ChatEvent>


    @GET("/message")
    suspend fun list(
        @Query("atVersion") atVersion: Int
    ): List<ChatEvent>

    @POST("/message/text")
    suspend fun sendText(
        @Header("Authorization") token: String,
        @Body message: ChatEvent
    ): ChatEvent

    @Multipart
    @POST("/message/image")
    suspend fun sendImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("event") event: RequestBody
    ): ChatEvent

    @POST("/message/seen")
    suspend fun sendSeen(
        @Header("Authorization") token: String,
        @Body message: ChatEvent
    ): ChatEvent

    @POST("/message/icon")
    suspend fun sendIcon(
        @Header("Authorization") token: String,
        @Body message: ChatEvent
    ): ChatEvent

}

fun Retrofit.eventCall(): EventCall = this.create(EventCall::class.java)