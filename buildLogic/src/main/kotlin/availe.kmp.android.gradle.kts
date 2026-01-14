import com.android.build.api.dsl.androidLibrary
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
val compileSdkVersion = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
val minSdkVersion = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

plugins {
    id("availe.kmp.targets")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        compileSdk = compileSdkVersion
        minSdk = minSdkVersion

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}