package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.NetworkConstants

internal object KeycloakDesiredState {
    const val REALM_NAME = KeycloakRealms.AVAILE
    const val DB_SCHEMA_NAME = KeycloakDbSchemas.KEYCLOAK

    val REALM_SETTINGS = DesiredRealmSettings()

    private const val HOST_IP = NetworkConstants.HOST_IP

    private val webAppRedirects = setOf(
        "http://$HOST_IP:3000/*",
        "https://$HOST_IP:3000/*",
        "http://$HOST_IP:8080/*"
    )

    private val mobileAppRedirects = setOf(
        "com.availe.auth://callback"
    )

    private val serviceRedirects = setOf(
        "http://$HOST_IP:8888/callback",
        "http://127.0.0.1:8888/callback"
    )

    private val allRedirectUris = webAppRedirects + mobileAppRedirects + serviceRedirects

    private val allWebOrigins = setOf(
        "http://$HOST_IP:3000",
        "https://$HOST_IP:3000",
        "http://$HOST_IP:8080"
    )

    val CLIENTS = listOf(
        DesiredClient(
            clientId = KeycloakClientIds.APP,
            clientName = "Availe App",
            isPublic = true,
            redirectUris = allRedirectUris,
            webOrigins = allWebOrigins,
            optionalClientScopes = listOf(
                KeycloakClientScopes.OFFLINE_ACCESS
            )
        )
    )

    val USER_PROFILE_ATTRIBUTES_TO_REMOVE = listOf(
        KeycloakUserAttributes.FIRST_NAME,
        KeycloakUserAttributes.LAST_NAME
    )
}