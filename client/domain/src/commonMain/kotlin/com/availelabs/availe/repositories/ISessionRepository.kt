package com.availelabs.availe.repositories

import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.UserProfile
import com.availelabs.availe.domain.model.AuthToken
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ISessionRepository {
    val authenticationState: StateFlow<AuthenticationState>
    val sessionInvalidated: SharedFlow<Unit>
    fun getAccountManagementUrl(): String
    suspend fun getCurrentUserProfile(): UserProfile?
    suspend fun onSessionInvalidated()
    suspend fun refreshAccessToken(): Boolean
    fun onLogoutSuccess()
    fun onTokenRefreshed(token: AuthToken)
    fun updateState(newAuthenticationState: AuthenticationState)
}