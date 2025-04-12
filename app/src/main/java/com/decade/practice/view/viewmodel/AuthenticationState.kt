package com.decade.practice.view.viewmodel

sealed class AuthenticationState(val message: String) {
    data object Success : AuthenticationState("Success")
    class Error(message: String) : AuthenticationState(message)
    data object Idle : AuthenticationState("")
    data object InProgress : AuthenticationState("In Progress")
}