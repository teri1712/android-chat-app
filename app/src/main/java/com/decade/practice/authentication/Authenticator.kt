package com.decade.practice.authentication

import android.net.Uri
import com.decade.practice.model.domain.Account
import com.decade.practice.model.domain.AccountEntry
import java.util.Date

interface Authenticator {

      @Throws(AuthenticationException::class)
      suspend fun signIn(username: String, password: String): AccountEntry

      @Throws(AuthenticationException::class)
      suspend fun signUp(
            username: String,
            password: String,
            fullname: String,
            gender: String,
            dob: Date,
            avatar: Uri?
      )

      suspend fun signInOAuth2(accessToken: String): AccountEntry
      suspend fun signOut(account: Account)
}
