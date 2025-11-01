package com.availelabs.availe.auth

sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val accessToken: String) : AuthState
}