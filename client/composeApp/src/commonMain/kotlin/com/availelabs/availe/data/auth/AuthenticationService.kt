package com.availelabs.availe.data.auth

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.domain.auth.AuthenticationConstants.CLIENT_ID
import com.availelabs.availe.domain.auth.AuthenticationConstants.DISCOVERY_URI
import com.availelabs.availe.domain.auth.AuthenticationConstants.SCOPE
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.auth.LoginResult
import com.availelabs.availe.domain.auth.LogoutError
import com.availelabs.availe.domain.auth.LogoutResult
import com.availelabs.availe.features.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.getTokens
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IAuthenticationService {
    val authenticationState: Value<AuthenticationState>
    suspend fun login(): LoginResult
    suspend fun logout(): LogoutResult
    fun getAccountManagementUrl(): String
    suspend fun getCurrentUserProfile(): UserProfile?
}

@OptIn(ExperimentalOpenIdConnect::class)
class AuthenticationService(
    private val authFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val platformAuthConfig: PlatformAuthenticationConfig,
    lifecycle: Lifecycle
) : IAuthenticationService {
    private val _authenticationState = MutableValue<AuthenticationState>(AuthenticationState.Unknown)
    override val authenticationState: Value<AuthenticationState> = _authenticationState

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        .withLifecycle(lifecycle)

    private val client = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
        this.scope = "$SCOPE offline_access"
        this.clientId = CLIENT_ID
        this.codeChallengeMethod = CodeChallengeMethod.S256
        this.redirectUri = platformAuthConfig.redirectUri
        this.postLogoutRedirectUri = platformAuthConfig.postLogoutRedirectUri
    }

    init {
        coroutineScope.launch {
            try {
                client.discover()
                val storedTokens = tokenStore.getTokens()

                if (storedTokens != null && storedTokens.refreshToken != null) {
                    try {
                        val newTokens = client.refreshToken(storedTokens.refreshToken!!)
                        tokenStore.saveTokens(newTokens)
                        _authenticationState.update { AuthenticationState.LoggedIn(newTokens.access_token) }
                    } catch (_: OpenIdConnectException) {
                        clearLocalTokens()
                    } catch (_: Exception) {
                        clearLocalTokens()
                    }
                } else {
                    _authenticationState.update { AuthenticationState.LoggedOut }
                }
            } catch (e: Exception) {
                _authenticationState.update { AuthenticationState.InitializationFailed(e) }
            }
        }
    }

    override fun getAccountManagementUrl(): String {
        return "http://${NetworkConstants.HOST_IP}:8180/realms/availe/account"
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val tokens = runCatching { tokenStore.getTokens() }.getOrNull() ?: return null
        val idToken = tokens.idToken ?: return null

        return JwtDecoder.decodeUserProfile(idToken)
    }

    private suspend fun clearLocalTokens() {
        try {
            tokenStore.removeTokens()
            _authenticationState.update { AuthenticationState.LoggedOut }
        } catch (_: Exception) {
            _authenticationState.update { AuthenticationState.LoggedOut }
        }
    }

    override suspend fun login(): LoginResult {
        return platformLogin(client, authFactory, tokenStore) { newState ->
            _authenticationState.update { newState }
        }
    }

    suspend fun resumeLogin(code: String) {
        platformResumeLogin(code, client, tokenStore) { newState ->
            _authenticationState.update { newState }
        }
    }

    override suspend fun logout(): LogoutResult {
        val result = try {
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

        clearLocalTokens()
        return result
    }
}

private object JwtDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    fun decodeUserProfile(token: String): UserProfile? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> "$payload=="
                3 -> "$payload="
                else -> payload
            }

            val decodedBytes = Base64.UrlSafe.decode(paddedPayload)
            val decodedString = decodedBytes.decodeToString()

            val dto = json.decodeFromString<JwtPayloadDto>(decodedString)
            dto.toUserProfile()
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun JwtPayloadDto.toUserProfile(): UserProfile {
    val id = Uuid.parse(this.sub)
    val email = this.email ?: throw IllegalArgumentException("Email claim is required")
    val picture = this.picture
    val name = this.name

    return if (name != null) {
        UserProfile.Named(
            id = id,
            email = email,
            pictureUrl = picture,
            name = name
        )
    } else {
        UserProfile.Anonymous(
            id = id,
            email = email,
            pictureUrl = picture
        )
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
expect suspend fun platformLogin(
    client: OpenIdConnectClient,
    authFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
): LoginResult

@OptIn(ExperimentalOpenIdConnect::class)
expect suspend fun platformResumeLogin(
    code: String,
    client: OpenIdConnectClient,
    tokenStore: TokenStore,
    onStateUpdate: (AuthenticationState) -> Unit
)