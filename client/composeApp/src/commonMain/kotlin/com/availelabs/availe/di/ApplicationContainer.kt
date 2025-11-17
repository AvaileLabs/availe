package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.*
import com.availelabs.availe.data.network.ApiClient
import com.availelabs.availe.data.network.IApiClient
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

interface ApplicationContainer {
    val authenticationService: IAuthenticationService
    val apiClient: IApiClient
}

@OptIn(ExperimentalOpenIdConnect::class)
fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): ApplicationContainer {
    val authFactory = createPlatformAuthenticationFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthConfig = createPlatformAuthenticationConfig(activity)

    val authenticationService = AuthenticationService(
        authFactory = authFactory,
        tokenStore = tokenStore,
        platformAuthConfig = platformAuthConfig,
        lifecycle = lifecycle
    )

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
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

    val apiClient = ApiClient(httpClient, json)

    return DefaultApplicationContainer(
        authenticationService = authenticationService,
        apiClient = apiClient
    )
}

private class DefaultApplicationContainer(
    override val authenticationService: IAuthenticationService,
    override val apiClient: IApiClient
) : ApplicationContainer