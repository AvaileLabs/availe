@file:OptIn(ExperimentalOpenIdConnect::class)

package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.AuthScope
import com.availelabs.availe.data.auth.createPlatformAuthenticationConfig
import com.availelabs.availe.data.auth.createPlatformAuthenticationFlowFactory
import com.availelabs.availe.data.auth.toScopeString
import com.availelabs.availe.data.repositories.*
import com.availelabs.availe.data.sources.local.ClientDatabaseFactory
import com.availelabs.availe.data.sources.local.ListingLocalDataSource
import com.availelabs.availe.data.sources.local.createSqlDriverFactory
import com.availelabs.availe.data.sources.local.createTokenStore
import com.availelabs.availe.data.sources.remote.AuthenticationRemoteDataSource
import com.availelabs.availe.data.sources.remote.ListingRemoteDataSource
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

interface IApplicationContainer {
    val sessionManager: ISessionManager
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
}

fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): IApplicationContainer {
    val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val sqlDriverFactory = createSqlDriverFactory(activity)
    val codeAuthenticationFlowFactory = createPlatformAuthenticationFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthenticationConfiguration = createPlatformAuthenticationConfig(activity)

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    val clientDatabaseDeferred = applicationCoroutineScope.async {
        val sqlDriver = sqlDriverFactory.createDriver()
        ClientDatabaseFactory.createClientDatabase(sqlDriver, json)
    }

    val listingLocalDataSource = ListingLocalDataSource(clientDatabaseDeferred)

    val openIdConnectClient =
        OpenIdConnectClient(discoveryUri = com.availelabs.availe.domain.auth.AuthenticationConstants.DISCOVERY_URI) {
            this.scope = setOf(
                AuthScope.OPEN_ID,
                AuthScope.EMAIL,
                AuthScope.OFFLINE_ACCESS
            ).toScopeString()
            this.clientId = com.availelabs.availe.domain.auth.AuthenticationConstants.CLIENT_ID
            this.codeChallengeMethod = CodeChallengeMethod.S256
            this.redirectUri = platformAuthenticationConfiguration.redirectUri
            this.postLogoutRedirectUri = platformAuthenticationConfiguration.postLogoutRedirectUri
        }

    val authenticationRemoteDataSource = AuthenticationRemoteDataSource(
        openIdConnectClient = openIdConnectClient,
        codeAuthenticationFlowFactory = codeAuthenticationFlowFactory,
        tokenStore = tokenStore
    )

    val sessionManager = SessionManager(
        authenticationRemoteDataSource = authenticationRemoteDataSource,
        lifecycle = lifecycle,
        accountManagementUrl = "http://${NetworkConstants.HOST_IP}:8180/realms/availe/account"
    )

    val authenticationRepository = AuthenticationRepository(
        authenticationRemoteDataSource = authenticationRemoteDataSource,
        sessionManager = sessionManager
    )

    val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 10_000
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val tokens = authenticationRemoteDataSource.getTokens()
                    if (tokens?.accessToken != null && tokens.refreshToken != null) {
                        BearerTokens(tokens.accessToken, tokens.refreshToken!!)
                    } else {
                        null
                    }
                }

                refreshTokens {
                    val oldRefreshToken = oldTokens?.refreshToken
                    if (oldRefreshToken != null) {
                        try {
                            val newTokens = authenticationRemoteDataSource.refreshTokens(oldRefreshToken)
                            authenticationRemoteDataSource.saveTokens(newTokens)
                            BearerTokens(newTokens.access_token, newTokens.refresh_token ?: oldRefreshToken)
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                host = NetworkConstants.HOST_IP
                port = SharedNetworkConstants.SERVER_API_PORT
                path("/api/")
            }
        }
    }

    val listingRemoteDataSource = ListingRemoteDataSource(
        httpClient = httpClient,
        json = json
    )

    val listingStoreFactory = ListingStoreFactory(
        remoteDataSource = listingRemoteDataSource,
        localDataSource = listingLocalDataSource
    )

    val listingRepository = ListingRepository(
        remoteDataSource = listingRemoteDataSource,
        localDataSource = listingLocalDataSource,
        storeFactory = listingStoreFactory
    )

    return ApplicationContainer(
        sessionManager = sessionManager,
        authenticationRepository = authenticationRepository,
        listingRepository = listingRepository
    )
}

private class ApplicationContainer(
    override val sessionManager: ISessionManager,
    override val authenticationRepository: IAuthenticationRepository,
    override val listingRepository: IListingRepository
) : IApplicationContainer