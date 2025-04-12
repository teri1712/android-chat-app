package com.decade.practice.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit

interface HttpContext {
    val client: OkHttpClient
    val retrofit: Retrofit
}
