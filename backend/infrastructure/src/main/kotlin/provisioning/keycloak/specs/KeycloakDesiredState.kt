package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.NetworkConstants

internal data class DesiredRealmSettings(
    val accessTokenLifespan: Int = 300,
    val ssoSessionIdleTimeout: Int = 1800,
    val ssoSessionMaxLifespan: Int = 36000,
    val offlineSessionIdleTimeout: Int = 2592000,
    val registrationAllowed: Boolean = true,
    val resetPasswordAllowed: Boolean = true,
    val rememberMe: Boolean = true,
    val loginWithEmailAllowed: Boolean = true,
    val registrationEmailAsUsername: Boolean = true,
    val duplicateEmailsAllowed: Boolean = false,
    val verifyEmail: Boolean = false,
    val bruteForceProtected: Boolean = true,
    val permanentLockout: Boolean = false,
    val maxFailureWaitSeconds: Int = 900,
    val failureFactor: Int = 30
)

internal data class DesiredClient(
    val clientId: String,
    val clientName: String,
    val isPublic: Boolean = false,
    val isStandardFlowEnabled: Boolean = true,
    val isDirectAccessGrantsEnabled: Boolean = true,
    val redirectUris: Set<String> = emptySet(),
    val webOrigins: Set<String> = emptySet(),
    val defaultClientScopes: List<String> = listOf(
        KeycloakClientScopes.PROFILE,
        KeycloakClientScopes.EMAIL,
        KeycloakClientScopes.ROLES,
        KeycloakClientScopes.WEB_ORIGINS
    ),
    val attributes: Map<String, String> = mapOf(
        KeycloakClientAttributes.PKCE_METHOD to KeycloakClientAttributes.PKCE_METHOD_S256
    )
)

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