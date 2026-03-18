import com.android.build.api.dsl.androidLibrary
import com.availelabs.buildlogic.configureIosFramework

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.availelabs"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe"
    }

    configureIosFramework()

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.logic)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}