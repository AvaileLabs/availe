package com.availelabs.provisioners.spec

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
    val defaultClientScopes: List<String> = listOf("profile", "email", "roles", "web-origins"),
    val attributes: Map<String, String> = mapOf(
        "pkce.code.challenge.method" to "S256"
    )
)

internal object KeycloakDesiredState {
    const val REALM_NAME = "availe"
    const val DB_SCHEMA_NAME = "keycloak"

    val REALM_SETTINGS = DesiredRealmSettings()

    val CLIENTS = listOf(
        DesiredClient(
            clientId = "availe-shop-app",
            clientName = "Availe Shop App",
            isPublic = true,
            redirectUris = setOf(
                "http://localhost:3000/*",
                "https://localhost:3000/*",
                "http://localhost:8888/callback",
                "com.availe.auth://callback",
                "http://localhost:8080/*"
            ),
            webOrigins = setOf("http://localhost:3000", "https://localhost:3000", "http://localhost:8080")
        )
    )
}