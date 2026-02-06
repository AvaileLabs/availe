import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.availe.kmp.sourcesets.nonAndroidNonWeb)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
}

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe.features.chat"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)
            implementation(projects.client.core)

            implementation(projects.client.features.auth)
            implementation(projects.client.features.chat)
            implementation(projects.client.features.home)
            implementation(projects.client.features.listing)
            implementation(projects.client.features.profile)
            implementation(projects.client.features.session)

            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)

            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.utils)

            // Decompose
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            implementation(libs.decompose.extensions.compose.experimental)
        }

        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}