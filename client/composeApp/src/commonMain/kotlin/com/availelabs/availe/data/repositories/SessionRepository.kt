package com.availelabs.availe.data.repositories

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.data.sources.remote.IAuthenticationRemoteDataSource
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
    fun onLogoutSuccess()
    fun updateState(newAuthenticationState: AuthenticationState)
}

class SessionManager(
    private val authenticationRemoteDataSource: IAuthenticationRemoteDataSource,
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
                authenticationRemoteDataSource.discover()
                val storedTokens = authenticationRemoteDataSource.getTokens()

                if (storedTokens != null && storedTokens.refreshToken != null) {
                    try {
                        val newTokens = authenticationRemoteDataSource.refreshTokens(storedTokens.refreshToken!!)
                        authenticationRemoteDataSource.saveTokens(newTokens)
                        _authenticationState.update { AuthenticationState.LoggedIn(newTokens.access_token) }
                    } catch (_: OpenIdConnectException) {
                        handleExpiredSession()
                    } catch (_: Exception) {
                        handleExpiredSession()
                    }
                } else {
                    _authenticationState.update { AuthenticationState.LoggedOut }
                }
            } catch (exception: Exception) {
                _authenticationState.update { AuthenticationState.InitializationFailed(exception) }
            }
        }
    }

    override fun getAccountManagementUrl(): String {
        return accountManagementUrl
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val tokens = runCatching { authenticationRemoteDataSource.getTokens() }.getOrNull() ?: return null
        val idToken = tokens.idToken ?: return null
        return JwtUserProfileMapper.decodeUserProfile(idToken)
    }

    override suspend fun onSessionInvalidated() {
        authenticationRemoteDataSource.removeTokens()
        _authenticationState.update { AuthenticationState.SessionExpired }
    }

    override fun onLogoutSuccess() {
        _authenticationState.update { AuthenticationState.LoggedOut }
    }

    override fun updateState(newAuthenticationState: AuthenticationState) {
        _authenticationState.update { newAuthenticationState }
    }

    private suspend fun handleExpiredSession() {
        try {
            authenticationRemoteDataSource.removeTokens()
        } catch (_: Exception) {
        }
        _authenticationState.update { AuthenticationState.SessionExpired }
    }
}