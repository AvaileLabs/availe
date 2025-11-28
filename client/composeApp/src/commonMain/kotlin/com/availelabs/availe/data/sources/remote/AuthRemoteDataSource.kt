package com.availelabs.availe.data.auth

import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutError
import com.availelabs.availe.domain.auth.LogoutResult
import kotlinx.coroutines.CancellationException
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.*
import org.publicvalue.multiplatform.oidc.types.remote.AccessTokenResponse

interface IAuthRemoteDataSource {
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
class AuthRemoteDataSource(
    private val client: OpenIdConnectClient,
    private val authFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore
) : IAuthRemoteDataSource {

    override suspend fun discover() {
        client.discover()
    }

    override suspend fun getTokens(): OauthTokens? {
        return tokenStore.getTokens()
    }

    override suspend fun refreshTokens(refreshToken: String): AccessTokenResponse {
        return client.refreshToken(refreshToken)
    }

    override suspend fun saveTokens(tokens: AccessTokenResponse) {
        tokenStore.saveTokens(tokens)
    }

    override suspend fun removeTokens() {
        tokenStore.removeTokens()
    }

    override suspend fun login(onStateUpdate: (AuthenticationState) -> Unit): LoginResult {
        return platformLogin(client, authFactory, tokenStore, onStateUpdate)
    }

    override suspend fun resumeLogin(code: String, onStateUpdate: (AuthenticationState) -> Unit) {
        platformResumeLogin(code, client, tokenStore, onStateUpdate)
    }

    override suspend fun logout(): LogoutResult {
        return try {
            val idToken = tokenStore.getTokens()?.idToken
            if (idToken != null) {
                val flow = authFactory.createEndSessionFlow(client)
                flow.endSession(idToken)
            }
            LogoutResult.Success
        } catch (_: CancellationException) {
            LogoutResult.Cancelled
        } catch (e: OpenIdConnectException) {
            LogoutResult.Failed(LogoutError.Authentication(e.message, e.cause))
        } catch (e: Exception) {
            LogoutResult.Failed(LogoutError.Unknown(e))
        }
    }
}