import com.android.build.api.dsl.androidLibrary
import com.availelabs.buildlogic.configureIosFramework

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.availelabs"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe.xmpp"
    }

    configureIosFramework()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.core)
            implementation(libs.ktor.client.core)
            implementation(libs.xmlutil)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}