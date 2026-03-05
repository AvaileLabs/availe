package com.availelabs.availe.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.auth.*
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.database.DatabaseProvider
import com.availelabs.availe.database.createDatabaseFileController
import com.availelabs.availe.database.createSqlDriverFactory
import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.network.SafeNetworkCaller
import com.availelabs.availe.platform.PlatformActivity
import com.availelabs.availe.preferences.DatabasePreferences
import com.availelabs.availe.preferences.ListingPreferences
import com.availelabs.availe.repositories.*
import com.availelabs.availe.repositories.store.ListingStoreFactory
import com.availelabs.availe.services.MessageService
import com.availelabs.availe.sources.local.ListingLocalDataSource
import com.availelabs.availe.sources.local.LocalDataCleaner
import com.availelabs.availe.sources.remote.*
import com.availelabs.availe.xmpp.session.XmppSession
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import kotlin.time.Duration.Companion.milliseconds

object DataLayerFactory {
    @OptIn(ExperimentalOpenIdConnect::class)
    fun create(
        activity: PlatformActivity,
        instanceKeeper: InstanceKeeper
    ): IDataContainer {
        return instanceKeeper.getOrCreate {
            val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

            val sqlDriverFactory = createSqlDriverFactory(activity)
            val databaseFileController = createDatabaseFileController(activity)
            val codeAuthenticationFlowFactory = createPlatformAuthenticationFlowFactory(activity)
            val tokenStore = createTokenStore(activity)
            val platformAuthenticationConfiguration = createPlatformAuthenticationConfig(activity)
            val settings = Settings()
            val listingPreferences = ListingPreferences(settings)
            val databasePreferences = DatabasePreferences(settings)

            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            }

            val databaseProvider = DatabaseProvider(
                sqlDriverFactory = sqlDriverFactory,
                json = json,
                databasePreferences = databasePreferences,
                databaseFileController = databaseFileController
            )

            val listingLocalDataSource = ListingLocalDataSource(databaseProvider)

            val localDataCleaner = LocalDataCleaner(
                tokenStore = tokenStore,
                listingPreferences = listingPreferences,
                databaseProvider = databaseProvider,
                databaseFileController = databaseFileController
            )

            val safeNetworkCaller = SafeNetworkCaller(localDataCleaner)

            val openIdConnectClient = OpenIdConnectClient(discoveryUri = AuthenticationConstants.DISCOVERY_URI) {
                this.scope = setOf(
                    AuthScope.OPEN_ID,
                    AuthScope.PROFILE,
                    AuthScope.EMAIL,
                    AuthScope.OFFLINE_ACCESS
                ).toScopeString()
                this.clientId = AuthenticationConstants.CLIENT_ID
                this.codeChallengeMethod = CodeChallengeMethod.S256
                this.redirectUri = platformAuthenticationConfiguration.redirectUri
                this.postLogoutRedirectUri = platformAuthenticationConfiguration.postLogoutRedirectUri
            }

            val authenticationRemoteDataSource = AuthenticationRemoteDataSource(
                openIdConnectClient = openIdConnectClient,
                codeAuthenticationFlowFactory = codeAuthenticationFlowFactory,
                tokenStore = tokenStore
            )

            val sessionRepository = SessionRepository(
                authenticationRemoteDataSource = authenticationRemoteDataSource,
                localDataCleaner = localDataCleaner,
                lifecycle = LifecycleRegistry(),
                accountManagementUrl = AuthenticationConstants.ACCOUNT_MANAGEMENT_URL
            )

            val authenticationRepository = AuthenticationRepository(
                authenticationRemoteDataSource = authenticationRemoteDataSource,
                sessionManager = sessionRepository
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

                install(WebSockets) {
                    pingInterval = 20_000.milliseconds
                }

                install(Auth) {
                    bearer {
                        sendWithoutRequest { true }

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
                                    sessionRepository.onTokenRefreshed(AuthToken(newTokens.access_token))
                                    BearerTokens(newTokens.access_token, newTokens.refresh_token ?: oldRefreshToken)
                                } catch (_: Exception) {
                                    sessionRepository.onSessionInvalidated()
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

            val userRemoteDataSource = UserRemoteDataSource(
                httpClient = httpClient,
                json = json
            )

            val interactionRemoteDataSource = InteractionRemoteDataSource(
                httpClient = httpClient,
                json = json
            )

            val xmppSession = XmppSession(httpClient)

            val messagingRemoteDataSource = MessagingRemoteDataSource(
                xmppSession = xmppSession
            )

            val messagingRepository = MessagingRepository(
                messagingRemoteDataSource = messagingRemoteDataSource,
                databaseProvider = databaseProvider,
                sessionManager = sessionRepository,
                scope = applicationScope
            )

            val listingStoreFactory = ListingStoreFactory(
                remoteDataSource = listingRemoteDataSource,
                localDataSource = listingLocalDataSource
            )

            val listingRepository = ListingRepository(
                remoteDataSource = listingRemoteDataSource,
                localDataSource = listingLocalDataSource,
                storeFactory = listingStoreFactory,
                listingPreferences = listingPreferences,
                safeNetworkCaller = safeNetworkCaller
            )

            val userRepository = UserRepository(
                remoteDataSource = userRemoteDataSource,
                safeNetworkCaller = safeNetworkCaller
            )

            val interactionRepository = InteractionRepository(
                remoteDataSource = interactionRemoteDataSource,
                localDataSource = listingLocalDataSource,
                safeNetworkCaller = safeNetworkCaller
            )

            val messageService = MessageService(applicationScope)

            DataContainer(
                sessionRepository = sessionRepository,
                authenticationRepository = authenticationRepository,
                listingRepository = listingRepository,
                messagingRepository = messagingRepository,
                userRepository = userRepository,
                interactionRepository = interactionRepository,
                messageService = messageService,
                applicationScope = applicationScope
            )
        }
    }

    private class DataContainer(
        override val sessionRepository: SessionRepository,
        override val authenticationRepository: AuthenticationRepository,
        override val listingRepository: ListingRepository,
        override val messagingRepository: MessagingRepository,
        override val userRepository: UserRepository,
        override val interactionRepository: InteractionRepository,
        override val messageService: MessageService,
        private val applicationScope: CoroutineScope
    ) : IDataContainer, InstanceKeeper.Instance {
        override fun onDestroy() {
            applicationScope.cancel()
        }
    }
}