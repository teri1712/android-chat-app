package com.decade.practice.endpoints

import com.decade.practice.model.domain.AccountEntry
import com.decade.practice.model.domain.Credential
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthenticationCall {

      @POST("/login")
      @FormUrlEncoded
      suspend fun signIn(
            @Field("username") username: String,
            @Field("password") password: String
      ): AccountEntry

      @FormUrlEncoded
      @POST("/authentication/oauth2/token/login")
      suspend fun signIn(
            @Header("Oauth2-AccessToken") accessToken: String
      ): AccountEntry

      @POST("/logout")
      @FormUrlEncoded
      suspend fun signOut(
            @Field("refresh_token") refreshToken: String
      ): ResponseBody

      @POST("/authentication/sign-up")
      @Multipart
      suspend fun signUp(
            @Part("information") information: RequestBody,
      ): ResponseBody

      @POST("/authentication/sign-up")
      @Multipart
      suspend fun signUp(
            @Part("information") information: RequestBody,
            @Part file: MultipartBody.Part,
      ): ResponseBody

      @POST("/authentication/refresh-token")
      @FormUrlEncoded
      fun refresh(@Field("refresh_token") body: String): Credential
}

fun Retrofit.authenticationCall(): AuthenticationCall = this.create(AuthenticationCall::class.java)
