package com.availelabs.availe.sources.remote

import com.availelabs.availe.auth.*
import com.availelabs.availe.network.NetworkErrorType
import com.availelabs.availe.network.classifyNetworkError
import kotlinx.coroutines.CancellationException
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.*
import org.publicvalue.multiplatform.oidc.types.remote.AccessTokenResponse

internal interface IAuthenticationRemoteDataSource {
    suspend fun discover()
    suspend fun getTokens(): OauthTokens?
    suspend fun refreshTokens(refreshToken: String): AccessTokenResponse
    suspend fun saveTokens(tokens: AccessTokenResponse)
    suspend fun removeTokens()

    suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult
    suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit)
    suspend fun logout(): LogoutResult
}

@OptIn(ExperimentalOpenIdConnect::class)
internal class AuthenticationRemoteDataSource(
    private val openIdConnectClient: OpenIdConnectClient,
    private val codeAuthenticationFlowFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore
) : IAuthenticationRemoteDataSource {

    override suspend fun discover() =
        openIdConnectClient.discover()


    override suspend fun getTokens(): OauthTokens? =
        tokenStore.getTokens()


    override suspend fun refreshTokens(refreshToken: String): AccessTokenResponse =
        openIdConnectClient.refreshToken(refreshToken)


    override suspend fun saveTokens(tokens: AccessTokenResponse) =
        tokenStore.saveTokens(tokens)


    override suspend fun removeTokens() =
        tokenStore.removeTokens()


    override suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult =

        try {
            platformLogin(openIdConnectClient, codeAuthenticationFlowFactory, tokenStore, onStateUpdate)
        } catch (_: CancellationException) {
            LoginResult.Cancelled
        } catch (throwable: Throwable) {
            val errorMessage = throwable.message ?: ""
            if (errorMessage == "Socket closed" || errorMessage.contains("cancelled", ignoreCase = true)) {
                return LoginResult.Cancelled
            }

            val networkErrorType = throwable.classifyNetworkError()
            val loginError = when (networkErrorType) {
                is NetworkErrorType.Recoverable.Connectivity -> LoginError.NoInternetConnection
                is NetworkErrorType.Recoverable.ConnectionTimeout -> LoginError.ServerUnreachable
                is NetworkErrorType.Recoverable.ServerSide -> LoginError.ServerUnreachable
                is NetworkErrorType.Recoverable.ClientSide -> LoginError.Unexpected(
                    throwable.message ?: "Client side error"
                )

                is NetworkErrorType.Recoverable.DataSerialization -> LoginError.Unexpected("Data serialization error")
                is NetworkErrorType.Unrecoverable -> LoginError.Unexpected(throwable.message ?: "Unknown error")
            }
            LoginResult.Failed(loginError)
        }


    override suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit) =
        platformResumeLogin(code, openIdConnectClient, tokenStore, onStateUpdate)


    override suspend fun logout(): LogoutResult =
        try {
            val oauthTokens = tokenStore.getTokens()
            val idToken = oauthTokens?.idToken

            tokenStore.removeTokens()

            if (idToken != null) {
                val endSessionFlow = codeAuthenticationFlowFactory.createEndSessionFlow(openIdConnectClient)
                endSessionFlow.endSession(idToken)
            }
            LogoutResult.Success
        } catch (_: CancellationException) {
            LogoutResult.Cancelled
        } catch (openIdConnectException: OpenIdConnectException) {
            LogoutResult.Failed(
                LogoutError.Authentication(
                    message = openIdConnectException.message,
                    cause = openIdConnectException.cause
                )
            )
        } catch (exception: Exception) {
            LogoutResult.Failed(LogoutError.Unknown(exception))
        }

}