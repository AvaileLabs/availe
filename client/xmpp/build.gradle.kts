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
        namespace = "com.availelabs.xmpp"
    }

    configureIosFramework()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.xmlutil)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}