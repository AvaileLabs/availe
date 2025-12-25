package com.availelabs.availe.repositories

import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.LoginResult
import com.availelabs.availe.auth.LogoutResult
import com.availelabs.availe.sources.remote.IAuthenticationRemoteDataSource

class AuthenticationRepository(
    private val authenticationRemoteDataSource: IAuthenticationRemoteDataSource,
    private val sessionManager: ISessionManager
) : IAuthenticationRepository {

    override suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult {
        return authenticationRemoteDataSource.login(onStateUpdate)
    }

    override suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit) {
        authenticationRemoteDataSource.resumeLogin(code, onStateUpdate)
    }

    override suspend fun logout(): LogoutResult {
        val logoutResult = authenticationRemoteDataSource.logout()
        sessionManager.onLogoutSuccess()
        return logoutResult
    }
}