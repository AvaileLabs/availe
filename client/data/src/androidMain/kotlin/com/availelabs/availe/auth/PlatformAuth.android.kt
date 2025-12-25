package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.network.NetworkErrorType
import com.availelabs.availe.network.classifyNetworkError
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
    val accessTokenResponse = try {
        val authFlow = authFactory.createAuthFlow(client)
        authFlow.getAccessToken()
    } catch (_: CancellationException) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Cancelled
    } catch (exception: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)

        val errorMessage = exception.message ?: ""
        if (errorMessage.contains("canceled", ignoreCase = true) || errorMessage.contains(
                "cancelled",
                ignoreCase = true
            )
        ) {
            return LoginResult.Cancelled
        }

        val networkErrorType = exception.classifyNetworkError()
        val loginError = when (networkErrorType) {
            is NetworkErrorType.Recoverable.Connectivity -> LoginError.NoInternetConnection
            is NetworkErrorType.Recoverable.ConnectionTimeout -> LoginError.ServerUnreachable
            is NetworkErrorType.Recoverable.ServerSide -> LoginError.ServerUnreachable
            else -> LoginError.Unexpected(exception.message ?: "Unknown Android login error")
        }

        return LoginResult.Failed(loginError)
    }

    try {
        tokenStore.saveTokens(accessTokenResponse)
    } catch (exception: Exception) {
        onStateUpdate(AuthenticationState.LoggedOut)
        return LoginResult.Failed(LoginError.Unexpected("Token storage failed: ${exception.message}"))
    }

    onStateUpdate(AuthenticationState.LoggedIn(AuthToken(accessTokenResponse.access_token)))
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