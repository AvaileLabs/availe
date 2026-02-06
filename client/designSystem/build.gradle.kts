import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.availe.kmp.sourcesets.nonWeb)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
}

kotlin {
    androidLibrary {
        namespace = "com.availelabs.designsystem"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            api(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)

            // Compose Design System
            api(libs.composables.unstyled)
            api(libs.composables.icons.lucide)

            // coil3
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
                exclude("org.jetbrains.compose.material3")
            }
        }
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

compose {
    resources {
        publicResClass = true
    }
}