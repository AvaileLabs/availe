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
            val storedTokens = runCatching { authenticationRemoteDataSource.getTokens() }.getOrNull()
            val hasLocalSession = storedTokens?.accessToken != null
            val storedUserId = storedTokens?.idToken?.let { JwtUserProfileMapper.decodeUserProfile(it) }?.id

            if (hasLocalSession && storedUserId != null) {
                val optimisticToken = AuthToken(storedTokens.accessToken)
                updateState(
                    AuthenticationState.LoggedIn(
                        accessToken = optimisticToken,
                        userId = storedUserId,
                        isOffline = false
                    )
                )
            }

            try {
                authenticationRemoteDataSource.discover()
            } catch (exception: Exception) {
                if (hasLocalSession && storedUserId != null) {
                    val token = AuthToken(storedTokens.accessToken)
                    updateState(
                        AuthenticationState.LoggedIn(
                            accessToken = token,
                            userId = storedUserId,
                            isOffline = true
                        )
                    )
                    return@launch
                } else {
                    updateState(AuthenticationState.InitializationFailed(exception))
                    return@launch
                }
            }

            if (hasLocalSession && storedTokens.refreshToken != null && storedUserId != null) {
                try {
                    val newTokens = authenticationRemoteDataSource.refreshTokens(storedTokens.refreshToken!!)
                    authenticationRemoteDataSource.saveTokens(newTokens)

                    val newToken = AuthToken(newTokens.access_token)
                    val newUserId =
                        newTokens.id_token?.let { JwtUserProfileMapper.decodeUserProfile(it) }?.id ?: storedUserId

                    updateState(
                        AuthenticationState.LoggedIn(
                            accessToken = newToken,
                            userId = newUserId,
                            isOffline = false
                        )
                    )
                } catch (_: OpenIdConnectException) {
                    handleExpiredSession()
                } catch (_: Exception) {
                    val token = AuthToken(storedTokens.accessToken)
                    updateState(
                        AuthenticationState.LoggedIn(
                            accessToken = token,
                            userId = storedUserId,
                            isOffline = true
                        )
                    )
                }
            } else if (!hasLocalSession) {
                updateState(AuthenticationState.LoggedOut)
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

    override fun onTokenRefreshed(token: AuthToken) {
        val currentState = _authenticationState.value
        if (currentState is AuthenticationState.LoggedIn) {
            updateState(currentState.copy(accessToken = token))
        }
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