package com.availelabs.availe.domain.auth

sealed interface AuthenticationState {
    data object Unknown : AuthenticationState
    data object LoggedOut : AuthenticationState
    data class LoggedIn(val accessToken: String) : AuthenticationState
}