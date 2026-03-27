package com.availelabs.orchestration.postgresejabberd

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.utils.loadGradleProperties

internal data class PostgresEjabberdConfig(
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val databaseName: String
)

internal fun loadPostgresEjabberdConfig(): PostgresEjabberdConfig {
    val gradleProperties = loadGradleProperties()

    val databaseName = InfrastructureRegistry.Topology.EJABBERD_DATABASE_NAME
    val databasePort = SharedNetworkConstants.EJABBERD_POSTGRES_PORT

    val databaseUrl = "jdbc:postgresql://localhost:$databasePort/$databaseName"

    return PostgresEjabberdConfig(
        databaseUrl = databaseUrl,
        databaseUser = gradleProperties.getProperty("ejabberd.database.user")
            ?: error("No ejabberd.database.user found in gradle.properties"),
        databasePassword = gradleProperties.getProperty("ejabberd.database.password")
            ?: error("No ejabberd.database.password found in gradle.properties"),
        databaseName = databaseName
    )
}