package com.availelabs.availe.repositories

import com.arkivanov.decompose.value.Value
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.UserProfile
import kotlinx.coroutines.flow.SharedFlow

interface ISessionManager {
    val authenticationState: Value<AuthenticationState>
    val sessionInvalidated: SharedFlow<Unit>
    fun getAccountManagementUrl(): String
    suspend fun getCurrentUserProfile(): UserProfile?
    suspend fun onSessionInvalidated()
    fun onLogoutSuccess()
    fun updateState(newAuthenticationState: AuthenticationState)
}