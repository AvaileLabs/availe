package com.availelabs.orchestration.hostsystem

import com.availelabs.utils.loadGradleProperties

internal data class HostSystemConfig(
    val sudoPassword: String
)

internal fun loadHostSystemConfig(): HostSystemConfig {
    val gradleProperties = loadGradleProperties()
    return HostSystemConfig(
        sudoPassword = gradleProperties.getProperty("system.sudo.password")
            ?: error("No system.sudo.password found in gradle.properties")
    )
}