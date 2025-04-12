package com.decade.practice.session

interface AccountLifecycle {
    suspend fun onStart() {}
    suspend fun onResume() {}
    suspend fun onLogout() {}
}