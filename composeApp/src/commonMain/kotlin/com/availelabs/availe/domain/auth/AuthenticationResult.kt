package com.availelabs.availe.domain.auth

sealed interface LoginResult {
    data object Success : LoginResult
    data object Cancelled : LoginResult
    data class Failed(val error: Throwable) : LoginResult
}

sealed interface LogoutResult {
    data object Success : LogoutResult
    data class Failed(val error: Throwable) : LogoutResult
}