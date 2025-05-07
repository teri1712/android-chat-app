package com.decade.practice.authentication

import android.net.Uri
import com.decade.practice.model.domain.Account
import com.decade.practice.model.domain.AccountEntry
import com.decade.practice.model.dto.SignUpRequest

interface Authenticator {

      @Throws(AuthenticationException::class)
      suspend fun signIn(username: String, password: String): AccountEntry

      @Throws(AuthenticationException::class)
      suspend fun signUp(
            information: SignUpRequest,
            avatar: Uri?
      )

      suspend fun signInOAuth2(accessToken: String): AccountEntry
      suspend fun signOut(account: Account)
}
