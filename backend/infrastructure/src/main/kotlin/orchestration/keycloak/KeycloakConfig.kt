package com.availelabs.orchestration.keycloak

import com.availelabs.utils.loadGradleProperties

internal data class KeycloakConfig(
    val adminUser: String,
    val adminPassword: String
)

internal fun loadKeycloakConfig(): KeycloakConfig {
    val props = loadGradleProperties()
    return KeycloakConfig(
        adminUser = props.getProperty("keycloak.admin.user")
            ?: error("No keycloak.admin.user found in gradle.properties"),
        adminPassword = props.getProperty("keycloak.admin.password")
            ?: error("No keycloak.admin.password found in gradle.properties")
    )
}