package com.availelabs.availe.repositories

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.JwtUserProfileMapper
import com.availelabs.availe.auth.UserProfile
import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.sources.local.ILocalDataCleaner
import com.availelabs.availe.sources.remote.IAuthenticationRemoteDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.publicvalue.multiplatform.oidc.OpenIdConnectException

internal class SessionRepository(
    private val authenticationRemoteDataSource: IAuthenticationRemoteDataSource,
    private val localDataCleaner: ILocalDataCleaner,
    lifecycle: Lifecycle,
    private val accountManagementUrl: String
) : ISessionRepository {

    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    override val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()

    private val _sessionInvalidated = MutableSharedFlow<Unit>()
    override val sessionInvalidated: SharedFlow<Unit> = _sessionInvalidated.asSharedFlow()

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
                        val authToken = AuthToken(newTokens.access_token)
                        updateState(AuthenticationState.LoggedIn(authToken))
                    } catch (_: OpenIdConnectException) {
                        handleExpiredSession()
                    } catch (_: Exception) {
                        handleExpiredSession()
                    }
                } else {
                    updateState(AuthenticationState.LoggedOut)
                }
            } catch (exception: Exception) {
                updateState(AuthenticationState.InitializationFailed(exception))
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
        localDataCleaner.clearAllData()
        updateState(AuthenticationState.SessionExpired)
        _sessionInvalidated.emit(Unit)
    }

    override fun onLogoutSuccess() {
        updateState(AuthenticationState.LoggedOut)
    }

    override fun updateState(newAuthenticationState: AuthenticationState) {
        if (newAuthenticationState == _authenticationState.value) return

        coroutineScope.launch(Dispatchers.Main) {
            _authenticationState.update { newAuthenticationState }
        }
    }

    private suspend fun handleExpiredSession() {
        localDataCleaner.clearAllData()
        withContext(Dispatchers.Main) {
            updateState(AuthenticationState.SessionExpired)
            _sessionInvalidated.emit(Unit)
        }
    }
}