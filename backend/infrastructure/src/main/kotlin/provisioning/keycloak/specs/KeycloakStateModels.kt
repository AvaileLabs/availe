package com.availelabs.provisioning.keycloak.specs

import org.keycloak.representations.idm.ProtocolMapperRepresentation

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
        KeycloakClientScopes.EMAIL,
        KeycloakClientScopes.ROLES,
        KeycloakClientScopes.WEB_ORIGINS
    ),
    val optionalClientScopes: List<String> = emptyList(),
    val attributes: Map<String, String> = mapOf(
        KeycloakClientAttributes.PKCE_METHOD to KeycloakClientAttributes.PKCE_METHOD_S256
    ),
    val protocolMappers: List<ProtocolMapperRepresentation> = emptyList()
)