package com.decade.practice.authentication

import android.content.Context
import android.net.Uri
import com.decade.practice.cacheFile
import com.decade.practice.endpoints.authenticationCall
import com.decade.practice.model.domain.Account
import com.decade.practice.model.domain.AccountEntry
import com.decade.practice.model.dto.SignUpRequest
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationService @Inject constructor(
      private val retrofit: Retrofit,
      private val context: Context,
      private val gson: Gson
) : Authenticator {

      @Throws(AuthenticationException::class)
      override suspend fun signIn(username: String, password: String): AccountEntry = withContext(Dispatchers.IO) {
            try {
                  retrofit.authenticationCall().signIn(username, password)
            } catch (e: HttpException) {
                  throw AuthenticationException(e.response()?.errorBody()?.string() ?: e.message ?: "Authentication Failed")
            }
      }

      @Throws(AuthenticationException::class)
      override suspend fun signUp(
            information: SignUpRequest,
            avatar: Uri?
      ): Unit = withContext(Dispatchers.IO) {
            try {
                  val informationPart = RequestBody.create(
                        "application/json".toMediaType(),
                        gson.toJson(information)
                  )
                  if (avatar == null) {
                        retrofit.authenticationCall().signUp(informationPart)
                        return@withContext
                  }
                  val file = context.cacheFile(avatar)
                  val requestFile = RequestBody.create(MultipartBody.FORM, file)
                  val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                  retrofit.authenticationCall().signUp(
                        informationPart,
                        avatarPart
                  )
            } catch (e: HttpException) {
                  throw AuthenticationException(
                        e.response()
                              ?.errorBody()
                              ?.string() ?: e.message ?: "Authentication Failed"
                  )
            }
      }

      @Throws(AuthenticationException::class)
      override suspend fun signInOAuth2(accessToken: String): AccountEntry = withContext(Dispatchers.IO) {
            try {
                  retrofit.authenticationCall().signIn(accessToken) ?: throw AuthenticationException()
            } catch (e: HttpException) {
                  throw AuthenticationException(e.response()?.errorBody()?.string() ?: e.message ?: "Authentication Failed")
            }
      }

      override suspend fun signOut(account: Account): Unit =
            withContext(Dispatchers.IO) {
                  val refresh = account.credential.refreshToken
                  retrofit.authenticationCall().signOut(refresh)
            }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthenticationModule {
      @Binds
      abstract fun authenticator(authenticationService: AuthenticationService): Authenticator
}