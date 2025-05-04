package com.decade.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.model.presentation.AuthenticationState
import com.decade.practice.session.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
      private val accountRepository: AccountRepository
) : ViewModel() {

      private val _loginState = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
      val loginState = _loginState.asStateFlow()

      fun signIn(username: String, password: String) {
            if (_loginState.value == AuthenticationState.InProgress)
                  return
            _loginState.value = AuthenticationState.InProgress
            assert(loginState.value == AuthenticationState.InProgress)
            viewModelScope.launch {
                  try {
                        accountRepository.logIn(username, password)
                        _loginState.value = AuthenticationState.Success
                  } catch (e: AuthenticationException) {
                        _loginState.value = AuthenticationState.Error(e.message)
                        e.printStackTrace()
                  }
            }
      }

      fun signIn(accessToken: String) {
            if (_loginState.value == AuthenticationState.InProgress)
                  return
            _loginState.value = AuthenticationState.InProgress
            viewModelScope.launch {
                  try {
                        accountRepository.logIn(accessToken)
                        _loginState.value = AuthenticationState.Success
                  } catch (e: AuthenticationException) {
                        e.printStackTrace()
                        _loginState.value = AuthenticationState.Error(e.message)
                  }
            }
      }
}



