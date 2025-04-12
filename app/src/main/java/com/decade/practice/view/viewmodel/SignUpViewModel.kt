package com.decade.practice.view.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.authentication.Authenticator
import com.decade.practice.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
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
        dob: Date,
        uri: Uri?
    ) {
        if (_signUpState.value == AuthenticationState.InProgress)
            return
        _signUpState.value = AuthenticationState.InProgress
        viewModelScope.launch {
            try {
                authenticator.signUp(username, password, fullname, gender, dob, uri)
                accountRepository.logIn(username, password)
                _signUpState.value = AuthenticationState.Success
            } catch (e: AuthenticationException) {
                _signUpState.value = AuthenticationState.Error(e.message)
                e.printStackTrace()
            }
        }
    }

}