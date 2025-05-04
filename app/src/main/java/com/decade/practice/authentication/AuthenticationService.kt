package com.decade.practice.authentication

import android.content.Context
import android.net.Uri
import com.decade.practice.cacheFile
import com.decade.practice.endpoints.authenticationCall
import com.decade.practice.model.domain.Account
import com.decade.practice.model.domain.AccountEntry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationService @Inject constructor(
      private val retrofit: Retrofit,
      private val context: Context
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
            username: String,
            password: String,
            fullname: String,
            gender: String,
            dob: Date,
            avatar: Uri?
      ): Unit = withContext(Dispatchers.IO) {
            try {
                  val dobString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dob)
                  if (avatar == null) {
                        retrofit.authenticationCall().signUp(
                              username.toRequestBody("text/plain".toMediaTypeOrNull()),
                              password.toRequestBody("text/plain".toMediaTypeOrNull()),
                              fullname.toRequestBody("text/plain".toMediaTypeOrNull()),
                              gender.toRequestBody("text/plain".toMediaTypeOrNull()),
                              dobString
                        )
                        return@withContext
                  }
                  val file = context.cacheFile(avatar)
                  val requestFile = RequestBody.create(MultipartBody.FORM, file)
                  val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                  retrofit.authenticationCall().signUp(
                        username.toRequestBody("text/plain".toMediaTypeOrNull()),
                        password.toRequestBody("text/plain".toMediaTypeOrNull()),
                        fullname.toRequestBody("text/plain".toMediaTypeOrNull()),
                        gender.toRequestBody("text/plain".toMediaTypeOrNull()),
                        dobString,
                        avatarPart
                  )
            } catch (e: HttpException) {
                  throw AuthenticationException(e.response()?.errorBody()?.string() ?: e.message ?: "Authentication Failed")
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

      override suspend fun signOut(account: Account) = withContext(Dispatchers.IO) {
//        val accessToken = account.credential.accessToken
//        retrofit.authenticationCall().signOut("Bearer $accessToken", account)
      }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthenticationModule {
      @Binds
      abstract fun authenticator(authenticationService: AuthenticationService): Authenticator
}