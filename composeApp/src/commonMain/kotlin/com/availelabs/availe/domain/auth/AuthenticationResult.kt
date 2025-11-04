package com.availelabs.availe.domain.auth

sealed interface LoginResult {
    data object Success : LoginResult
    data object Cancelled : LoginResult
    data class Failed(val error: LoginError) : LoginResult
}

sealed interface LoginError {
    data class Authentication(val message: String, val cause: Throwable?) : LoginError
    data class Storage(val cause: Throwable) : LoginError
    data class Unknown(val cause: Throwable) : LoginError
}

sealed interface LogoutResult {
    data object Success : LogoutResult
    data object Cancelled : LogoutResult
    data class Failed(val error: LogoutError) : LogoutResult
}

sealed interface LogoutError {
    data class Authentication(val message: String, val cause: Throwable?) : LogoutError
    data class Storage(val cause: Throwable) : LogoutError
    data class Unknown(val cause: Throwable) : LogoutError
}