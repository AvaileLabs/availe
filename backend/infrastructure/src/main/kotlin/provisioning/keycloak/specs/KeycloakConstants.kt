package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.orchestration.InfrastructureRegistry

internal object KeycloakRealms {
    const val AVAILE = SharedAuthenticationConstants.REALM_NAME
}

internal object KeycloakDbSchemas {
    const val KEYCLOAK = InfrastructureRegistry.Topology.DATABASE_SCHEMA_KEYCLOAK
}

internal object KeycloakClientIds {
    const val APP = SharedAuthenticationConstants.CLIENT_ID
}

internal object KeycloakClientScopes {
    const val EMAIL = SharedAuthenticationConstants.Scopes.EMAIL
    const val PROFILE = SharedAuthenticationConstants.Scopes.PROFILE
    const val ROLES = SharedAuthenticationConstants.Scopes.ROLES
    const val WEB_ORIGINS = SharedAuthenticationConstants.Scopes.WEB_ORIGINS
    const val OFFLINE_ACCESS = SharedAuthenticationConstants.Scopes.OFFLINE_ACCESS
}

internal object KeycloakClientAttributes {
    const val PKCE_METHOD = "pkce.code.challenge.method"
    const val PKCE_METHOD_S256 = "S256"
}

internal object KeycloakUserAttributes {
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
}