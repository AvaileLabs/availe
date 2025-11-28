package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.*
import com.availelabs.availe.data.repositories.*
import com.availelabs.availe.data.sources.local.createTokenStore
import com.availelabs.availe.data.sources.remote.ListingRemoteDataSource
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

interface ApplicationContainer {
    val sessionManager: ISessionManager
    val authRepository: IAuthRepository
    val listingRepository: IListingRepository
}

@OptIn(ExperimentalOpenIdConnect::class)
fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): ApplicationContainer {
    val authFactory = createPlatformAuthenticationFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthConfig = createPlatformAuthenticationConfig(activity)

    val client =
        OpenIdConnectClient(discoveryUri = com.availelabs.availe.domain.auth.AuthenticationConstants.DISCOVERY_URI) {
            this.scope = setOf(
                AuthScope.OPEN_ID,
                AuthScope.PROFILE,
                AuthScope.EMAIL,
                AuthScope.OFFLINE_ACCESS
            ).toScopeString()
            this.clientId = com.availelabs.availe.domain.auth.AuthenticationConstants.CLIENT_ID
            this.codeChallengeMethod = CodeChallengeMethod.S256
            this.redirectUri = platformAuthConfig.redirectUri
            this.postLogoutRedirectUri = platformAuthConfig.postLogoutRedirectUri
        }

    val authRemoteDataSource = AuthRemoteDataSource(
        client = client,
        authFactory = authFactory,
        tokenStore = tokenStore
    )

    val sessionManager = SessionManager(
        authRemoteDataSource = authRemoteDataSource,
        lifecycle = lifecycle,
        accountManagementUrl = "http://${NetworkConstants.HOST_IP}:8180/realms/availe/account"
    )

    val authRepository = AuthRepository(authRemoteDataSource)

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    sessionManager.onSessionInvalidated()
                    throw CancellationException("Unauthorized access")
                }
            }
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                host = NetworkConstants.HOST_IP
                port = 9002
                path("/api/")
            }
        }
    }

    val listingRemoteDataSource = ListingRemoteDataSource(
        httpClient = httpClient,
        json = json
    )

    val listingRepository = ListingRepository(listingRemoteDataSource)

    return DefaultApplicationContainer(
        sessionManager = sessionManager,
        authRepository = authRepository,
        listingRepository = listingRepository
    )
}

private class DefaultApplicationContainer(
    override val sessionManager: ISessionManager,
    override val authRepository: IAuthRepository,
    override val listingRepository: IListingRepository
) : ApplicationContainer