package com.decade.practice.authentication

import java.io.IOException

class AuthenticationException(override val message: String = "UNAUTHORIZED") : IOException(message)
class NoCredentialException : IOException("NO_CREDENTIAL")
