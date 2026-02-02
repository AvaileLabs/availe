import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.availelabs.features.session"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)

            implementation(libs.runtime)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}