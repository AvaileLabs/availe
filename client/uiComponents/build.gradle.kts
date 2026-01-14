import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.availe.kmp.sourcesets.nonAndroidNonWeb)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
}

kotlin {
    androidLibrary {
        namespace = "com.availelabs.uicomponents"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(projects.client.designSystem)
            implementation(projects.client.domain)
            implementation(projects.client.core)

            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)

            // Compose Design System
            implementation(libs.composables.unstyled)
            implementation(libs.composables.icons.lucide)

            // coil3
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            // Decompose
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            implementation(libs.decompose.extensions.compose.experimental)

            // MVIKotlin
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
                exclude("org.jetbrains.compose.material3")
            }
        }
    }
}