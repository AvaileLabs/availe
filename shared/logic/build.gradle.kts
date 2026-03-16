import com.android.build.api.dsl.androidLibrary
import com.availelabs.buildlogic.HostIpValueSource
import com.github.gmazzo.buildconfig.BuildConfigSourceSet

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
    alias(libs.plugins.gradle.buildconfig)
}

val useLocalIp = providers.gradleProperty("availe.dev.useLocalIp").getOrElse("false").toBoolean()

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe.logic"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val hostIpProvider = providers.provider {
    if (useLocalIp) {
        val ipAddress = providers.of(HostIpValueSource::class) {}.get()
        "\"$ipAddress\""
    } else {
        "\"api.availe.io\""
    }
}

val xmppDomainProvider = providers.provider {
    if (useLocalIp) {
        "\"localhost\""
    } else {
        "\"xmpp.availe.io\""
    }
}

val useLocalIpProvider = providers.provider {
    useLocalIp.toString()
}

buildConfig {
    packageName("com.availelabs.availe")
    className("NetworkConstants")

    useKotlinOutput {
        internalVisibility = false
    }

    sourceSets.named<BuildConfigSourceSet>("main") {
        buildConfigField("String", "HOST_IP", hostIpProvider)
        buildConfigField("String", "XMPP_DOMAIN", xmppDomainProvider)
        buildConfigField("Boolean", "USE_LOCAL_IP", useLocalIpProvider)
    }
}