package com.availelabs.availe.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.availelabs.availe.auth.AuthConstants.CLIENT_ID
import com.availelabs.availe.auth.AuthConstants.DISCOVERY_URI
import com.availelabs.availe.auth.AuthConstants.POST_LOGOUT_REDIRECT_URI_ANDROID
import com.availelabs.availe.auth.AuthConstants.POST_LOGOUT_REDIRECT_URI_IOS
import com.availelabs.availe.auth.AuthConstants.POST_LOGOUT_REDIRECT_URI_JVM
import com.availelabs.availe.auth.AuthConstants.POST_LOGOUT_REDIRECT_URI_WEB
import com.availelabs.availe.auth.AuthConstants.REDIRECT_URI_ANDROID
import com.availelabs.availe.auth.AuthConstants.REDIRECT_URI_IOS
import com.availelabs.availe.auth.AuthConstants.REDIRECT_URI_JVM
import com.availelabs.availe.auth.AuthConstants.REDIRECT_URI_WEB
import com.availelabs.availe.auth.AuthConstants.SCOPE
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

@OptIn(ExperimentalOpenIdConnect::class)
open class AuthService(
    private val authFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    coroutineScope: CoroutineScope
) {
    private val _authState = MutableValue<AuthState>(AuthState.Unknown)
    open val authState: Value<AuthState> = _authState

    private val client = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
        this.scope = SCOPE
        this.clientId = CLIENT_ID
        this.codeChallengeMethod = CodeChallengeMethod.S256
        this.redirectUri = getPlatformRedirectUri()
        this.postLogoutRedirectUri = getPlatformPostLogoutRedirectUri()
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

    suspend fun login() {
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

    suspend fun logout() {
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

    private fun getPlatformRedirectUri(): String {
        return when (authFactory::class.simpleName) {
            "AndroidCodeAuthFlowFactory" -> REDIRECT_URI_ANDROID
            "IosCodeAuthFlowFactory" -> REDIRECT_URI_IOS
            "JvmCodeAuthFlowFactory" -> REDIRECT_URI_JVM
            "WebMainCodeAuthFlowFactory" -> REDIRECT_URI_WEB
            "WasmCodeAuthFlowFactory" -> REDIRECT_URI_WEB
            else -> REDIRECT_URI_WEB
        }
    }

    private fun getPlatformPostLogoutRedirectUri(): String {
        return when (authFactory::class.simpleName) {
            "AndroidCodeAuthFlowFactory" -> POST_LOGOUT_REDIRECT_URI_ANDROID
            "IosCodeAuthFlowFactory" -> POST_LOGOUT_REDIRECT_URI_IOS
            "JvmCodeAuthFlowFactory" -> POST_LOGOUT_REDIRECT_URI_JVM
            "WebMainCodeAuthFlowFactory" -> POST_LOGOUT_REDIRECT_URI_WEB
            "WasmCodeAuthFlowFactory" -> POST_LOGOUT_REDIRECT_URI_WEB
            else -> POST_LOGOUT_REDIRECT_URI_WEB
        }
    }
}