package com.availelabs.availe.setup

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.postgresql.PGProperty
import java.util.concurrent.TimeUnit

internal fun hikariSetup(): HikariDataSource {
    val gradleProperties = loadUserGradleProperties()

    val hikariConfiguration = HikariConfig().apply {
        jdbcUrl = gradleProperties.getProperty("db.url") ?: error("No db.url found in gradle.properties")
        username = gradleProperties.getProperty("db.user") ?: error("No db.user found in gradle.properties")
        password = gradleProperties.getProperty("db.password") ?: error("No db.password found in gradle.properties")

        addDataSourceProperty(PGProperty.TCP_KEEP_ALIVE.getName(), true)

        keepaliveTime = TimeUnit.MINUTES.toMillis(3)
        maxLifetime = TimeUnit.MINUTES.toMillis(30)
    }

    return HikariDataSource(hikariConfiguration)
}