package com.availelabs.availe.setup

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.postgresql.PGProperty
import java.io.File
import java.util.*

internal fun hikariSetup(): HikariDataSource {
    val gradlePropertiesFilePath = File(System.getProperty("user.home"), ".gradle/gradle.properties")

    check(gradlePropertiesFilePath.exists()) {
        "Gradle properties file not found at ${gradlePropertiesFilePath.absolutePath}"
    }

    val props = Properties().apply {
        gradlePropertiesFilePath.reader().use { load(it) }
    }

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = props.getProperty("db.url") ?: error("No db.url found in gradle.properties")
        username = props.getProperty("db.user") ?: error("No db.user found in gradle.properties")
        password = props.getProperty("db.password") ?: error("No db.password found in gradle.properties")
        addDataSourceProperty(PGProperty.TCP_KEEP_ALIVE.getName(), true)
    }

    return HikariDataSource(hikariConfig)
}