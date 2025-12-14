package com.availelabs.orchestration.opensearch

import com.availelabs.utils.loadGradleProperties

internal data class OpenSearchConfig(
    val adminPassword: String
)

internal fun loadOpenSearchConfig(): OpenSearchConfig {
    val gradleProperties = loadGradleProperties()
    return OpenSearchConfig(
        adminPassword = gradleProperties.getProperty("opensearch.admin.password")
            ?: error("No opensearch.admin.password found in gradle.properties")
    )
}