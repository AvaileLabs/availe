package com.availelabs.provisioning.keycloak.specs

internal object KeycloakRealms {
    const val AVAILE = "availe"
}

internal object KeycloakDbSchemas {
    const val KEYCLOAK = "keycloak"
}

internal object KeycloakClientIds {
    const val APP = "availe-app"
}

internal object KeycloakClientScopes {
    const val EMAIL = "email"
    const val ROLES = "roles"
    const val WEB_ORIGINS = "web-origins"
    const val OFFLINE_ACCESS = "offline_access"
}

internal object KeycloakClientAttributes {
    const val PKCE_METHOD = "pkce.code.challenge.method"
    const val PKCE_METHOD_S256 = "S256"
}

internal object KeycloakUserAttributes {
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
}