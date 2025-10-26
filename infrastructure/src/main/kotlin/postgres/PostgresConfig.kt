package com.availelabs.postgres

import com.availelabs.containers.domain.ContainerRunSpecification
import com.availelabs.utils.loadGradleProperties

internal data class PostgresConfig(
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String
)

internal fun loadPostgresConfig(): PostgresConfig {
    val props = loadGradleProperties()

    return PostgresConfig(
        dbUrl = props.getProperty("db.url") ?: error("No db.url found in gradle.properties"),
        dbUser = props.getProperty("db.user") ?: error("No db.user found in gradle.properties"),
        dbPassword = props.getProperty("db.password") ?: error("No db.password found in gradle.properties")
    )
}

internal fun postgresRunSpec(): ContainerRunSpecification<PostgresPortsList, PostgresVolumesList> {
    val config = loadPostgresConfig()
    val dbName = config.dbUrl.substringAfterLast('/')

    val environmentVariables = listOf(
        "POSTGRES_DB=$dbName",
        "POSTGRES_USER=${config.dbUser}",
        "POSTGRES_PASSWORD=${config.dbPassword}"
    )

    return ContainerRunSpecification(
        name = "availe-postgres",
        image = "docker.io/library/postgres",
        tag = "18",
        environmentVariables = environmentVariables,
        portMappings = PostgresPortsList,
        volumes = PostgresVolumesList
    )
}