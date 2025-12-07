package com.availelabs.availe.data.auth

import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.model.AuthToken
import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.Pkce
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens
import org.publicvalue.multiplatform.oidc.types.AuthCodeRequest
import web.console.console
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformLogin(
    client: OpenIdConnectClient,
    authFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
): LoginResult {
    val authorizationRequest = client.createAuthorizationCodeRequest()
    AuthStorage.saveAuthParams(
        verifier = authorizationRequest.pkce.codeVerifier,
        state = authorizationRequest.state,
        nonce = authorizationRequest.nonce,
        redirectUri = authorizationRequest.config.redirectUri
    )
    window.location.href = authorizationRequest.url.toString()
    return LoginResult.Cancelled
}

@OptIn(ExperimentalOpenIdConnect::class, ExperimentalWasmJsInterop::class)
actual suspend fun platformResumeLogin(
    code: String,
    client: OpenIdConnectClient,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
) {
    val codeVerifier = AuthStorage.getVerifier()
    val authenticationState = AuthStorage.getState()
    val nonceValue = AuthStorage.getNonce()
    val redirectUniformResourceIdentifier = AuthStorage.getRedirectUri()

    if (codeVerifier == null || authenticationState == null || redirectUniformResourceIdentifier == null) {
        console.error("Missing auth params in storage")
        return
    }

    val proofKeyForCodeExchange = Pkce(
        codeVerifier = codeVerifier,
        codeChallengeMethod = client.config.codeChallengeMethod
    )
    val requestUrl = io.ktor.http.Url(redirectUniformResourceIdentifier)

    val authenticationCodeRequest = AuthCodeRequest(
        url = requestUrl,
        config = client.config,
        pkce = proofKeyForCodeExchange,
        state = authenticationState,
        nonce = nonceValue
    )

    try {
        val tokenResponse = client.exchangeToken(authenticationCodeRequest, code)
        tokenStore.saveTokens(tokenResponse)
        AuthStorage.clear()
        onStateUpdate(AuthenticationState.LoggedIn(AuthToken(tokenResponse.access_token)))

        window.history.replaceState(null, "", "/")
    } catch (exception: Exception) {
        console.error("Failed to exchange token: ${exception.message}")
        onStateUpdate(AuthenticationState.InitializationFailed(exception))
    }
}