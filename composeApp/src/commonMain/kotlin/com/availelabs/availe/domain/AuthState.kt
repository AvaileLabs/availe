package com.availelabs.availe.domain

sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val accessToken: String) : AuthState
}