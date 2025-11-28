package com.availelabs.availe.data.repositories

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.data.auth.IAuthRemoteDataSource
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.JwtUserProfileMapper
import com.availelabs.availe.domain.auth.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.OpenIdConnectException

interface ISessionManager {
    val authenticationState: Value<AuthenticationState>
    fun getAccountManagementUrl(): String
    suspend fun getCurrentUserProfile(): UserProfile?
    suspend fun onSessionInvalidated()
    fun updateState(newState: AuthenticationState)
}

class SessionManager(
    private val authRemoteDataSource: IAuthRemoteDataSource,
    lifecycle: Lifecycle,
    private val accountManagementUrl: String
) : ISessionManager {

    private val _authenticationState = MutableValue<AuthenticationState>(AuthenticationState.Unknown)
    override val authenticationState: Value<AuthenticationState> = _authenticationState

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        .withLifecycle(lifecycle)

    init {
        coroutineScope.launch {
            try {
                authRemoteDataSource.discover()
                val storedTokens = authRemoteDataSource.getTokens()

                if (storedTokens != null && storedTokens.refreshToken != null) {
                    try {
                        val newTokens = authRemoteDataSource.refreshTokens(storedTokens.refreshToken!!)
                        authRemoteDataSource.saveTokens(newTokens)
                        _authenticationState.update { AuthenticationState.LoggedIn(newTokens.access_token) }
                    } catch (_: OpenIdConnectException) {
                        handleExpiredSession()
                    } catch (_: Exception) {
                        handleExpiredSession()
                    }
                } else {
                    _authenticationState.update { AuthenticationState.LoggedOut }
                }
            } catch (e: Exception) {
                _authenticationState.update { AuthenticationState.InitializationFailed(e) }
            }
        }
    }

    override fun getAccountManagementUrl(): String {
        return accountManagementUrl
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val tokens = runCatching { authRemoteDataSource.getTokens() }.getOrNull() ?: return null
        val idToken = tokens.idToken ?: return null
        return JwtUserProfileMapper.decodeUserProfile(idToken)
    }

    override suspend fun onSessionInvalidated() {
        authRemoteDataSource.removeTokens()
        _authenticationState.update { AuthenticationState.SessionExpired }
    }

    override fun updateState(newState: AuthenticationState) {
        _authenticationState.update { newState }
    }

    private suspend fun handleExpiredSession() {
        try {
            authRemoteDataSource.removeTokens()
        } catch (_: Exception) {
            // Ignore storage errors during cleanup
        }
        _authenticationState.update { AuthenticationState.SessionExpired }
    }
}