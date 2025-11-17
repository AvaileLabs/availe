package com.availelabs.availe.data.auth

import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginError
import com.availelabs.availe.domain.auth.LoginResult
import kotlinx.coroutines.CancellationException
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformLogin(
    client: OpenIdConnectClient,
    authFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
): LoginResult {
    val response = try {
        val flow = authFactory.createAuthFlow(client)
        flow.getAccessToken()
    } catch (_: CancellationException) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Cancelled
    } catch (e: OpenIdConnectException) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Failed(LoginError.Authentication(e.message, e.cause))
    } catch (e: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Failed(LoginError.Unknown(e))
    }

    try {
        tokenStore.saveTokens(response)
    } catch (e: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Failed(LoginError.Storage(e))
    }

    onStateUpdate(AuthenticationState.LoggedIn(response.access_token))
    return LoginResult.Success
}

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformResumeLogin(
    code: String,
    client: OpenIdConnectClient,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
) {
    // No-op on iOS
}