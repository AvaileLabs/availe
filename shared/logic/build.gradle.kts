import com.availelabs.buildlogic.HostIpValueSource
import com.github.gmazzo.buildconfig.BuildConfigSourceSet
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
    alias(libs.plugins.gradle.buildconfig)
}

val useLocalIp = providers.gradleProperty("availe.dev.useLocalIp").getOrElse("false").toBoolean()

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.availelabs.availe.logic"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
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
        "\"localhost\""
    }
}

buildConfig {
    packageName("com.availelabs.availe")
    className("NetworkConstants")

    useKotlinOutput {
        internalVisibility = false
    }

    sourceSets.named<BuildConfigSourceSet>("main") {
        buildConfigField("String", "HOST_IP", hostIpProvider)
    }
}