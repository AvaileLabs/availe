import com.availelabs.buildlogic.configureIosFramework

plugins {
    alias(libs.plugins.availe.kmp.sourcesets.nonWeb)
}

group = "com.availelabs"
version = "1.0.0"

base {
    archivesName.set("availe-client-core")
}

kotlin {
    android {
        namespace = "com.availelabs.availe.core"
    }

    configureIosFramework()

    sourceSets {
        androidMain.dependencies {
            api(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(projects.client.domain)
            implementation(projects.shared.configuration)

            implementation(libs.kermit)

            // Decompose
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
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