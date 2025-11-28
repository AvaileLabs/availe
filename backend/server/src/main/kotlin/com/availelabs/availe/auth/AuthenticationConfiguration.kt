package com.availelabs.availe.auth

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.configuration.SharedNetworkConstants

internal object AuthenticationConfiguration {
    private const val KEYCLOAK_HOST_ADDRESS = NetworkConstants.HOST_IP
    private const val KEYCLOAK_HOST_PORT_NUMBER = SharedNetworkConstants.KEYCLOAK_PUBLIC_PORT
    private const val KEYCLOAK_REALM_NAME = SharedAuthenticationConstants.REALM_NAME

    const val JSON_WEB_KEY_SET_UNIFORM_RESOURCE_IDENTIFIER =
        "http://$KEYCLOAK_HOST_ADDRESS:$KEYCLOAK_HOST_PORT_NUMBER/realms/$KEYCLOAK_REALM_NAME${SharedAuthenticationConstants.Paths.OPENID_CONNECT_CERTIFICATES}"

    const val EXPECTED_TOKEN_ISSUER =
        "http://$KEYCLOAK_HOST_ADDRESS:$KEYCLOAK_HOST_PORT_NUMBER/realms/$KEYCLOAK_REALM_NAME"
}