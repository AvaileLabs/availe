package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.NetworkConstants

internal object KeycloakDesiredState {
    const val REALM_NAME = KeycloakRealms.AVAILE
    const val DB_SCHEMA_NAME = KeycloakDbSchemas.KEYCLOAK

    val REALM_SETTINGS = DesiredRealmSettings()

    private val hostIp = NetworkConstants.HOST_IP

    private val webAppRedirects = setOf(
        "http://$hostIp:3000/*",
        "https://$hostIp:3000/*",
        "http://$hostIp:8080/*"
    )

    private val mobileAppRedirects = setOf(
        "com.availe.auth://callback"
    )

    private val serviceRedirects = setOf(
        "http://$hostIp:8888/callback"
    )

    private val allRedirectUris = webAppRedirects + mobileAppRedirects + serviceRedirects

    private val allWebOrigins = setOf(
        "http://$hostIp:3000",
        "https://$hostIp:3000",
        "http://$hostIp:8080"
    )

    val CLIENTS = listOf(
        DesiredClient(
            clientId = KeycloakClientIds.APP,
            clientName = "Availe App",
            isPublic = true,
            redirectUris = allRedirectUris,
            webOrigins = allWebOrigins
        )
    )

    val USER_PROFILE_ATTRIBUTES_TO_REMOVE = listOf(
        KeycloakUserAttributes.FIRST_NAME,
        KeycloakUserAttributes.LAST_NAME
    )
}