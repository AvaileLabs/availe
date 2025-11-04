package com.availelabs.availe.data.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.domain.auth.AuthenticationConstants.CLIENT_ID
import com.availelabs.availe.domain.auth.AuthenticationConstants.DISCOVERY_URI
import com.availelabs.availe.domain.auth.AuthenticationConstants.SCOPE
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
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
            } catch (e: Exception) {
                _authenticationState.update { AuthenticationState.InitializationFailed(e) }
            }
        }
    }

    override suspend fun login(): LoginResult {
        return try {
            val flow = authFactory.createAuthFlow(client)
            val response = flow.getAccessToken()
            tokenStore.saveTokens(response)
            _authenticationState.update { AuthenticationState.LoggedIn(response.access_token) }
            LoginResult.Success
        } catch (_: CancellationException) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            LoginResult.Cancelled
        } catch (e: Exception) {
            _authenticationState.update { AuthenticationState.LoggedOut }
            LoginResult.Failed(e)
        }
    }

    override suspend fun logout(): LogoutResult {
        val result = try {
            val idToken = tokenStore.getTokens()?.idToken
            if (idToken != null) {
                val flow = authFactory.createEndSessionFlow(client)
                flow.endSession(idToken)
            }
            LogoutResult.Success
        } catch (e: Exception) {
            LogoutResult.Failed(e)
        }

        tokenStore.removeTokens()
        _authenticationState.update { AuthenticationState.LoggedOut }

        return result
    }
}