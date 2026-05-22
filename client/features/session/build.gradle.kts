plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.availelabs.availe.features.session"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)
            implementation(projects.client.core)

            implementation(libs.runtime)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.kermit)
        }
    }
}