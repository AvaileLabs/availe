import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

group = "com.availelabs"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
    androidLibrary {
        namespace = "com.availelabs.availe"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

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

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val androidMain by getting
        val jvmMain by getting
        val iosMain by getting
        val webMain by getting

        val nonWebMain by creating {
            dependsOn(commonMain)
        }

        androidMain.dependsOn(nonWebMain)
        jvmMain.dependsOn(nonWebMain)
        iosMain.dependsOn(nonWebMain)

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            // Decompose
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            implementation(libs.decompose.extensions.compose.experimental)
            implementation(libs.kotlinx.coroutines.core)

            // MVIKotlin
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
        }
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}