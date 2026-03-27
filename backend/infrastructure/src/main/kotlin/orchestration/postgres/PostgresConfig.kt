package com.availelabs.orchestration.postgres

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