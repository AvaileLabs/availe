package com.availelabs.postgres.domain

internal data class PostgresConfig(
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String
)