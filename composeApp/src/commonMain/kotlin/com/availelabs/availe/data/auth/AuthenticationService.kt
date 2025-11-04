package com.availelabs.availe.data.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.domain.auth.*
import com.availelabs.availe.domain.auth.AuthenticationConstants.CLIENT_ID
import com.availelabs.availe.domain.auth.AuthenticationConstants.DISCOVERY_URI
import com.availelabs.availe.domain.auth.AuthenticationConstants.SCOPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.getTokens
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import kotlin.coroutines.cancellation.CancellationException

interface IAuthenticationService {
    val authenticationState: Value<AuthenticationState>
    suspend fun login(): LoginResult
    suspend fun logout(): LogoutResult
}

@OptIn(ExperimentalOpenIdConnect::class)
class AuthenticationService(
    private val authFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val platformAuthConfig: PlatformAuthenticationConfig,
    lifecycle: Lifecycle
) : IAuthenticationService {
    private val _authenticationState = MutableValue<AuthenticationState>(AuthenticationState.Unknown)
    override val authenticationState: Value<AuthenticationState> = _authenticationState

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        .withLifecycle(lifecycle)

    private val client = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
        this.scope = SCOPE
        this.clientId = CLIENT_ID
        this.codeChallengeMethod = CodeChallengeMethod.S256
        this.redirectUri = platformAuthConfig.redirectUri
        this.postLogoutRedirectUri = platformAuthConfig.postLogoutRedirectUri
    }

    init {
        coroutineScope.launch {
            try {
                client.discover()
                val tokens = tokenStore.getTokens()
                if (tokens != null) {
                    _authenticationState.update { AuthenticationState.LoggedIn(tokens.accessToken) }
                } else {
                    _authenticationState.update { AuthenticationState.LoggedOut }
                }
            } catch (e: OpenIdConnectException) {
                _authenticationState.update { AuthenticationState.InitializationFailed(e) }
            } catch (e: Exception) {
                _authenticationState.update { AuthenticationState.InitializationFailed(e) }
            }
        }
    }

    override suspend fun login(): LoginResult {
        val response = try {
            val flow = authFactory.createAuthFlow(client)
            flow.getAccessToken()
        } catch (e: CancellationException) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            return LoginResult.Cancelled
        } catch (e: OpenIdConnectException) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            return LoginResult.Failed(LoginError.Authentication(e.message, e.cause))
        } catch (e: Exception) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            return LoginResult.Failed(LoginError.Unknown(e))
        }

        try {
            tokenStore.saveTokens(response)
        } catch (e: Exception) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            return LoginResult.Failed(LoginError.Storage(e))
        }

        _authenticationState.update { AuthenticationState.LoggedIn(response.access_token) }
        return LoginResult.Success
    }

    override suspend fun logout(): LogoutResult {
        val result = try {
            val idToken = tokenStore.getTokens()?.idToken
            if (idToken != null) {
                val flow = authFactory.createEndSessionFlow(client)
                flow.endSession(idToken)
            }
            LogoutResult.Success
        } catch (e: CancellationException) {
            LogoutResult.Cancelled
        } catch (e: OpenIdConnectException) {
            LogoutResult.Failed(LogoutError.Authentication(e.message, e.cause))
        } catch (e: Exception) {
            LogoutResult.Failed(LogoutError.Unknown(e))
        }

        try {
            tokenStore.removeTokens()
        } catch (e: Exception) {
            if (result is LogoutResult.Success) {
                _authenticationState.update { AuthenticationState.LoggedOut }
                return LogoutResult.Failed(LogoutError.Storage(e))
            }
        }

        _authenticationState.update { AuthenticationState.LoggedOut }

        return result
    }
}