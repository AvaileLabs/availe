package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.remote.IAuthenticationRemoteDataSource
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutResult

interface IAuthenticationRepository {
    suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult
    suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit)
    suspend fun logout(): LogoutResult
}

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