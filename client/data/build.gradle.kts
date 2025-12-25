import app.cash.sqldelight.gradle.SqlDelightDatabase
import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinSerialization)
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

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.logic)
            implementation(projects.client.core)
            implementation(projects.shared.configuration)
            implementation(projects.client.domain)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.store5)
            implementation(libs.xmlutil)

            // kmp-oidc
            implementation(libs.oidc.appsupport)
            implementation(libs.oidc.ktor)
            implementation(libs.oidc.preferences)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            // Decompose
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            implementation(libs.decompose.extensions.compose.experimental)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.utils)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        webMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlin.browser)

            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", libs.versions.sqljs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
        }

        jsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
        }

        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver.wasm.js)
        }
    }
}

sqldelight {
    databases {
        create("ClientDatabase", Action<SqlDelightDatabase> {
            packageName.set("com.availelabs.availe.db")
            generateAsync.set(true)
        })
    }
}