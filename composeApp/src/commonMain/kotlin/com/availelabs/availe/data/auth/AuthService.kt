package com.availelabs.availe.data.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.availelabs.availe.domain.AuthConstants.CLIENT_ID
import com.availelabs.availe.domain.AuthConstants.DISCOVERY_URI
import com.availelabs.availe.domain.AuthConstants.SCOPE
import com.availelabs.availe.domain.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.getTokens
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

data class PlatformAuthConfig(
    val redirectUri: String,
    val postLogoutRedirectUri: String,
)

expect fun createPlatformAuthConfig(activity: PlatformActivity): PlatformAuthConfig

interface IAuthService {
    val authState: Value<AuthState>
    suspend fun login()
    suspend fun logout()
}

@OptIn(ExperimentalOpenIdConnect::class)
class AuthService(
    private val authFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val platformAuthConfig: PlatformAuthConfig,
    coroutineScope: CoroutineScope
) : IAuthService {
    private val _authState = MutableValue<AuthState>(AuthState.Unknown)
    override val authState: Value<AuthState> = _authState

    private val client = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
        this.scope = SCOPE
        this.clientId = CLIENT_ID
        this.codeChallengeMethod = CodeChallengeMethod.S256
        this.redirectUri = platformAuthConfig.redirectUri
        this.postLogoutRedirectUri = platformAuthConfig.postLogoutRedirectUri
    }

    init {
        println("AuthService: Initializing...")
        coroutineScope.launch {
            println("AuthService: Discovering client config...")
            client.discover()
            println("AuthService: Discovery complete.")
            initialize()
        }
    }

    private suspend fun initialize() {
        println("AuthService: Checking for existing tokens...")
        val tokens = tokenStore.getTokens()
        if (tokens != null) {
            println("AuthService: Found existing tokens. User is LoggedIn.")
            _authState.update { AuthState.LoggedIn(tokens.accessToken) }
        } else {
            println("AuthService: No tokens found. User is LoggedOut.")
            _authState.update { AuthState.LoggedOut }
        }
    }

    override suspend fun login() {
        println("AuthService: login() called.")
        try {
            println("AuthService: Creating AuthFlow...")
            val flow = authFactory.createAuthFlow(client)
            println("AuthService: Calling flow.getAccessToken() (this starts the server and opens the browser)...")
            val response = flow.getAccessToken()
            println("AuthService: getAccessToken() SUCCESS. Saving tokens.")
            tokenStore.saveTokens(response)
            _authState.update { AuthState.LoggedIn(response.access_token) }
            println("AuthService: State updated to LoggedIn.")
        } catch (e: Exception) {
            println("AuthService: login() FAILED: ${e.message}")
            e.printStackTrace()
            _authState.update { AuthState.LoggedOut }
        }
    }

    override suspend fun logout() {
        println("AuthService: logout() called.")
        try {
            val idToken = tokenStore.getTokens()?.idToken
            if (idToken != null) {
                println("AuthService: Ending session with token...")
                val flow = authFactory.createEndSessionFlow(client)
                flow.endSession(idToken)
                println("AuthService: End session flow complete.")
            } else {
                println("AuthService: No ID token found, skipping end session call.")
            }
        } catch (e: Exception) {
            println("AuthService: logout() FAILED: ${e.message}")
            e.printStackTrace()
        } finally {
            println("AuthService: Removing tokens and setting state to LoggedOut.")
            tokenStore.removeTokens()
            _authState.update { AuthState.LoggedOut }
        }
    }
}