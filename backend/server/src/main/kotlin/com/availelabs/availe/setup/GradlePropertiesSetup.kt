package com.availelabs.availe.setup

import java.io.File
import java.util.*

internal fun loadUserGradleProperties(): Properties {
    val gradlePropertiesFilePath = File(System.getProperty("user.home"), ".gradle/gradle.properties")

    check(gradlePropertiesFilePath.exists()) {
        "Gradle properties file not found at ${gradlePropertiesFilePath.absolutePath}"
    }

    return Properties().apply {
        gradlePropertiesFilePath.reader().use { load(it) }
    }
}