package com.decade.practice.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.authentication.Authenticator
import com.decade.practice.model.dto.SignUpRequest
import com.decade.practice.model.presentation.AuthenticationState
import com.decade.practice.session.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
      private val accountRepository: AccountRepository,
      private val authenticator: Authenticator
) : ViewModel() {

      private var _signUpState = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
      val signUpState = _signUpState.asStateFlow()

      fun signUp(
            username: String,
            password: String,
            fullname: String,
            gender: String,
            dobDate: Date,
            uri: Uri?
      ) {
            if (_signUpState.value == AuthenticationState.InProgress)
                  return
            _signUpState.value = AuthenticationState.InProgress
            viewModelScope.launch {
                  try {
                        val dob = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dobDate)
                        authenticator.signUp(SignUpRequest(username, password, fullname, gender, dob), uri)
                        accountRepository.logIn(username, password)
                        _signUpState.value = AuthenticationState.Success
                  } catch (e: AuthenticationException) {
                        _signUpState.value = AuthenticationState.Error(e.message)
                        e.printStackTrace()
                  }
            }
      }

}