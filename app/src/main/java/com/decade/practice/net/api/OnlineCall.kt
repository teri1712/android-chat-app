package com.decade.practice.net.api

import com.decade.practice.model.Online
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface OnlineCall {

    @GET("/online")
    suspend fun list(): List<Online>

    @GET("/online/{username}")
    suspend fun get(@Path("username") username: String): Online
}

fun Retrofit.onlineCall(): OnlineCall = this.create(OnlineCall::class.java)
