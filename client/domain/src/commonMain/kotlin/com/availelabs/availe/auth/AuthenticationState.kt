package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.AuthToken

sealed interface AuthenticationState {
    data object Unknown : AuthenticationState
    data object LoggedOut : AuthenticationState
    data object SessionExpired : AuthenticationState
    data class LoggedIn(val accessToken: AuthToken) : AuthenticationState
    data class InitializationFailed(val error: Throwable) : AuthenticationState
}