package com.availelabs.orchestration.postgreskeycloak

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.utils.loadGradleProperties

internal data class PostgresKeycloakConfig(
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val databaseName: String
)

internal fun loadPostgresKeycloakConfig(): PostgresKeycloakConfig {
    val gradleProperties = loadGradleProperties()

    val databaseName = SharedNetworkConstants.KEYCLOAK_DB_NAME
    val databasePort = SharedNetworkConstants.KEYCLOAK_POSTGRES_PORT

    val databaseUrl = "jdbc:postgresql://localhost:$databasePort/$databaseName"

    return PostgresKeycloakConfig(
        databaseUrl = databaseUrl,
        databaseUser = gradleProperties.getProperty("keycloak.database.user")
            ?: error("No keycloak.database.user found in gradle.properties"),
        databasePassword = gradleProperties.getProperty("keycloak.database.password")
            ?: error("No keycloak.database.password found in gradle.properties"),
        databaseName = databaseName
    )
}