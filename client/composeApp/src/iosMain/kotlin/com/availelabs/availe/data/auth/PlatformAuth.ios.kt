package com.availelabs.availe.data.auth

import com.availelabs.availe.data.network.NetworkErrorType
import com.availelabs.availe.data.network.classifyNetworkError
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginError
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.model.AuthToken
import kotlinx.coroutines.CancellationException
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
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
    } catch (exception: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)

        val networkErrorType = exception.classifyNetworkError()
        val loginError = when (networkErrorType) {
            is NetworkErrorType.Recoverable.Connectivity -> LoginError.NoInternetConnection
            is NetworkErrorType.Recoverable.ConnectionTimeout -> LoginError.ServerUnreachable
            is NetworkErrorType.Recoverable.ServerSide -> LoginError.ServerUnreachable
            else -> LoginError.Unexpected(exception.message ?: "Unknown iOS login error")
        }

        return LoginResult.Failed(loginError)
    }

    try {
        tokenStore.saveTokens(response)
    } catch (exception: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Failed(LoginError.Unexpected("Token storage failed: ${exception.message}"))
    }

    onStateUpdate(AuthenticationState.LoggedIn(AuthToken(response.access_token)))
    return LoginResult.Success
}

@OptIn(ExperimentalOpenIdConnect::class)
actual suspend fun platformResumeLogin(
    code: String,
    client: OpenIdConnectClient,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
) {
}