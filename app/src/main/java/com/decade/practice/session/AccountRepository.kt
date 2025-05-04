package com.decade.practice.session

import com.decade.practice.authentication.AuthenticationException

interface AccountRepository {

      val currentSession: Session?
      val hasSession: Boolean
            get() = currentSession != null

      @Throws(AuthenticationException::class)
      suspend fun logIn(username: String, password: String): Session

      @Throws(AuthenticationException::class)
      suspend fun logIn(accessToken: String): Session
      suspend fun logOut()
}