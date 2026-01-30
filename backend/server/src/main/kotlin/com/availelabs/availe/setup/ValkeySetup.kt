package com.availelabs.availe.setup

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.data.cache.ValkeyCacheService

internal fun valkeyCacheServiceSetup(): ValkeyCacheService {
    val gradleProperties = loadUserGradleProperties()
    val valkeyPassword = gradleProperties.getProperty("valkey.password")
        ?: error("No valkey.password found in gradle.properties")

    return ValkeyCacheService(
        host = SharedNetworkConstants.VALKEY_HOST,
        port = SharedNetworkConstants.VALKEY_PORT,
        password = valkeyPassword
    )
}