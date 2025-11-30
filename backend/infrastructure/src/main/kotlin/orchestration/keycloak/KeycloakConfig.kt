package com.availelabs.orchestration.keycloak

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.postgres.loadPostgresConfig
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

internal fun keycloakRunSpec(): ContainerRunSpecification<KeycloakPortsList, KeycloakVolumesList> {
    val keycloakConfig = loadKeycloakConfig()
    val postgresConfig = loadPostgresConfig()
    val dbName = postgresConfig.dbUrl.substringAfterLast('/')

    val environmentVariables = listOf(
        "KEYCLOAK_ADMIN=${keycloakConfig.adminUser}",
        "KEYCLOAK_ADMIN_PASSWORD=${keycloakConfig.adminPassword}",
        "KC_DB=postgres",
        "KC_DB_URL_HOST=availe-postgres",
        "KC_DB_URL_DATABASE=$dbName",
        "KC_DB_USERNAME=${postgresConfig.dbUser}",
        "KC_DB_PASSWORD=${postgresConfig.dbPassword}",
        "KC_DB_SCHEMA=${SharedNetworkConstants.DATABASE_SCHEMA_KEYCLOAK}",
        "KC_HOSTNAME=${NetworkConstants.HOST_IP}",
        "KC_HEALTH_ENABLED=true",
        "KC_HTTP_PORT=${KeycloakPortsList.Http.containerPortNumber}",
        "KC_HTTPS_PORT=8443",
        "KC_MANAGEMENT_HTTP_PORT=${KeycloakPortsList.Management.containerPortNumber}"
    )

    return ContainerRunSpecification(
        name = "availe-keycloak",
        image = "quay.io/keycloak/keycloak",
        tag = "26.0.7",
        environmentVariables = environmentVariables,
        portMappings = KeycloakPortsList,
        volumes = KeycloakVolumesList,
        command = listOf("start-dev")
    )
}