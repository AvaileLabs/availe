package com.availelabs.availe.repositories

import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.LoginResult
import com.availelabs.availe.auth.LogoutResult
import com.availelabs.availe.domain.model.AuthToken

interface IAuthenticationRepository {
    suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult
    suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit)
    suspend fun logout(): LogoutResult
    suspend fun validateSession(token: AuthToken): Boolean
}