package com.availelabs.availe.data.auth

import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.Pkce
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens
import org.publicvalue.multiplatform.oidc.types.AuthCodeRequest
import web.console.console

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformLogin(
    client: OpenIdConnectClient,
    authFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
): LoginResult {
    // Fallback if called directly
    val request = client.createAuthorizationCodeRequest()
    AuthStorage.saveAuthParams(
        verifier = request.pkce.codeVerifier,
        state = request.state,
        nonce = request.nonce,
        redirectUri = request.config.redirectUri
    )
    window.location.href = request.url.toString()
    return LoginResult.Cancelled
}

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformResumeLogin(
    code: String,
    client: OpenIdConnectClient,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
) {
    val verifier = AuthStorage.getVerifier()
    val state = AuthStorage.getState()
    val nonce = AuthStorage.getNonce()

    if (verifier == null || state == null) {
        console.error("Missing auth params in storage")
        return
    }

    val pkce = Pkce(codeVerifier = verifier, codeChallengeMethod = client.config.codeChallengeMethod)
    val dummyUrl = io.ktor.http.Url("http://localhost")

    val request = AuthCodeRequest(
        url = dummyUrl,
        config = client.config,
        pkce = pkce,
        state = state,
        nonce = nonce
    )

    try {
        val response = client.exchangeToken(request, code)
        tokenStore.saveTokens(response)
        AuthStorage.clear()
        onStateUpdate(AuthenticationState.LoggedIn(response.access_token))

        window.history.replaceState(null, "", "/")
    } catch (e: Exception) {
        console.error("Failed to exchange token: ${e.message}")
        onStateUpdate(AuthenticationState.InitializationFailed(e))
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformGetLoginUrl(client: OpenIdConnectClient): String? {
    val request = client.createAuthorizationCodeRequest()

    AuthStorage.saveAuthParams(
        verifier = request.pkce.codeVerifier,
        state = request.state,
        nonce = request.nonce,
        redirectUri = request.config.redirectUri
    )

    return request.url.toString()
}

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformGetLogoutUrl(client: OpenIdConnectClient, idToken: String?): String? {
    val request = client.createEndSessionRequest(idToken)
    return request.url.toString()
}