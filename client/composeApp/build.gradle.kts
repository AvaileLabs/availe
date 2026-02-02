import com.android.build.api.dsl.androidLibrary
import com.availelabs.buildlogic.configureIosFramework
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
        namespace = "com.availelabs.availe"
    }

    configureIosFramework {
        export(projects.client.core)
        export(projects.shared.logic)
        export(projects.shared.configuration)
        export(projects.client.domain)
        export(projects.client.features.appShell)
        export(libs.decompose)
        export(libs.decompose.extensions.compose)
        export(libs.essenty.lifecycle)
        export(libs.essenty.lifecycle.coroutines)
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
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            api(projects.client.core)
            api(projects.shared.logic)
            api(projects.shared.configuration)
            implementation(libs.okio)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.client.core)
            api(projects.client.domain)
            implementation(projects.client.data)
            implementation(projects.client.designSystem)
            implementation(projects.client.uiComponents)
            api(projects.client.features.appShell)
            implementation(projects.client.features.session)

            // temporary
            implementation(projects.client.features.auth)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.utils)

            // Compose Design System
            implementation(libs.composables.unstyled)
            implementation(libs.composables.icons.lucide)

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

            // Ktor common
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // kotlinx serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // coil3
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
                exclude("org.jetbrains.compose.material3")
            }
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.logback)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlin.browser)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.availelabs.availe.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.availelabs.availe"
            packageVersion = "1.0.0"
        }
    }
}