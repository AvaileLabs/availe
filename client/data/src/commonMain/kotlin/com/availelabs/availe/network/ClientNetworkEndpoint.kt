package com.availelabs.availe.network

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import kotlin.jvm.JvmInline

@JvmInline
internal value class HostAddress(val value: String)

internal data class HttpEndpoint(
    val hostAddress: HostAddress,
    val port: Int,
)

internal data class XmppEndpoint(
    val hostAddress: HostAddress,
    val port: Int,
)

internal data class KeycloakEndpoint(
    val hostAddress: HostAddress,
    val port: Int,
) {
    val realmBaseUrl: String
        get() = "http://${hostAddress.value}:$port/realms/${SharedAuthenticationConstants.REALM_NAME}"
}

internal data class ClientNetworkEndpointSet(
    val httpEndpoint: HttpEndpoint,
    val xmppEndpoint: XmppEndpoint,
    val keycloakEndpoint: KeycloakEndpoint,
)

internal expect fun createClientNetworkEndpointSet(): ClientNetworkEndpointSet

internal object ClientNetworkEndpoint {
    val current: ClientNetworkEndpointSet by lazy(::createClientNetworkEndpointSet)
}

internal fun createClientNetworkEndpointSet(hostAddress: HostAddress): ClientNetworkEndpointSet {
    return ClientNetworkEndpointSet(
        httpEndpoint = HttpEndpoint(
            hostAddress = hostAddress,
            port = SharedNetworkConstants.SERVER_API_PORT,
        ),
        xmppEndpoint = XmppEndpoint(
            hostAddress = hostAddress,
            port = SharedNetworkConstants.EJABBERD_ADMIN_PORT,
        ),
        keycloakEndpoint = KeycloakEndpoint(
            hostAddress = hostAddress,
            port = SharedNetworkConstants.KEYCLOAK_PUBLIC_PORT,
        ),
    )
}