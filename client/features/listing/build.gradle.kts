import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
}

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe.features.listing"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)
            implementation(projects.client.features.session)
            implementation(projects.shared.configuration)
            implementation(projects.client.core)

            implementation(libs.decompose)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
            implementation(libs.kotlinx.datetime)

            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}