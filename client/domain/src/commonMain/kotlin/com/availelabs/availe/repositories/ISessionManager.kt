package com.availelabs.availe.repositories

import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.UserProfile
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ISessionManager {
    val authenticationState: StateFlow<AuthenticationState>
    val sessionInvalidated: SharedFlow<Unit>
    fun getAccountManagementUrl(): String
    suspend fun getCurrentUserProfile(): UserProfile?
    suspend fun onSessionInvalidated()
    fun onLogoutSuccess()
    fun updateState(newAuthenticationState: AuthenticationState)
}