package com.availelabs.orchestration.valkey

import com.availelabs.utils.loadGradleProperties

internal data class ValkeyConfig(
    val password: String
)

internal fun loadValkeyConfig(): ValkeyConfig {
    val properties = loadGradleProperties()
    return ValkeyConfig(
        password = properties.getProperty("valkey.password")
            ?: error("No valkey.password found in gradle.properties")
    )
}