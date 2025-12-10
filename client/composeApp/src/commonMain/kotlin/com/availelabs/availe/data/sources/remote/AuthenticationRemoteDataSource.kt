package com.availelabs.availe.data.sources.remote

import com.availelabs.availe.core.coroutines.IoDispatcher
import com.availelabs.availe.data.auth.platformLogin
import com.availelabs.availe.data.auth.platformResumeLogin
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutError
import com.availelabs.availe.domain.auth.LogoutResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.*
import org.publicvalue.multiplatform.oidc.types.remote.AccessTokenResponse

interface IAuthenticationRemoteDataSource {
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
class AuthenticationRemoteDataSource(
    private val openIdConnectClient: OpenIdConnectClient,
    private val codeAuthenticationFlowFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore
) : IAuthenticationRemoteDataSource {

    override suspend fun discover() = withContext(IoDispatcher) {
        openIdConnectClient.discover()
    }

    override suspend fun getTokens(): OauthTokens? = withContext(IoDispatcher) {
        tokenStore.getTokens()
    }

    override suspend fun refreshTokens(refreshToken: String): AccessTokenResponse = withContext(IoDispatcher) {
        openIdConnectClient.refreshToken(refreshToken)
    }

    override suspend fun saveTokens(tokens: AccessTokenResponse) = withContext(IoDispatcher) {
        tokenStore.saveTokens(tokens)
    }

    override suspend fun removeTokens() = withContext(IoDispatcher) {
        tokenStore.removeTokens()
    }

    override suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult =
        withContext(IoDispatcher) {
            platformLogin(openIdConnectClient, codeAuthenticationFlowFactory, tokenStore, onStateUpdate)
        }

    override suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit) =
        withContext(IoDispatcher) {
            platformResumeLogin(code, openIdConnectClient, tokenStore, onStateUpdate)
        }

    override suspend fun logout(): LogoutResult = withContext(IoDispatcher) {
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
}