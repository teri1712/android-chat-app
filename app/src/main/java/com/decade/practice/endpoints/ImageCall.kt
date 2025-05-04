package com.decade.practice.endpoints

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface ImageCall {

      @GET("/image")
      suspend fun download(
            @Query("filename") filename: String,
      ): ResponseBody

}

fun Retrofit.imageCall(): ImageCall = this.create(ImageCall::class.java)
