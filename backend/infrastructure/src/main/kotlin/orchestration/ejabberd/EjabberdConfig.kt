package com.availelabs.orchestration.ejabberd

import com.availelabs.utils.loadGradleProperties

internal data class EjabberdConfig(
    val adminUser: String,
    val adminPass: String,
    val databaseUser: String,
    val databasePassword: String
)

internal fun loadEjabberdConfig(): EjabberdConfig {
    val gradleProperties = loadGradleProperties()
    return EjabberdConfig(
        adminUser = gradleProperties.getProperty("ejabberd.admin.user")
            ?: error("No ejabberd.admin.user found in gradle.properties"),
        adminPass = gradleProperties.getProperty("ejabberd.admin.password")
            ?: error("No ejabberd.admin.password found in gradle.properties"),
        databaseUser = gradleProperties.getProperty("ejabberd.database.user")
            ?: error("No ejabberd.database.user found in gradle.properties"),
        databasePassword = gradleProperties.getProperty("ejabberd.database.password")
            ?: error("No ejabberd.database.password found in gradle.properties")
    )
}