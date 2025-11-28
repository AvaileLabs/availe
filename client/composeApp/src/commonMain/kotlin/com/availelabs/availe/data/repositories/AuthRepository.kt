package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.auth.IAuthRemoteDataSource
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutResult

interface IAuthRepository {
    suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult
    suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit)
    suspend fun logout(): LogoutResult
}

class AuthRepository(
    private val remoteDataSource: IAuthRemoteDataSource
) : IAuthRepository {

    override suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult {
        return remoteDataSource.login(onStateUpdate)
    }

    override suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit) {
        remoteDataSource.resumeLogin(code, onStateUpdate)
    }

    override suspend fun logout(): LogoutResult {
        return remoteDataSource.logout()
    }
}