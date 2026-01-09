import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
}

kotlin {
    jvmToolchain(21)
    androidLibrary {
        namespace = "com.availelabs.availe.features.profile"

        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()
    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)
            implementation(projects.client.features.session)
            implementation(projects.client.core)

            implementation(libs.decompose)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
        }
    }
}