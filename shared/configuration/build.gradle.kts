plugins {
    alias(libs.plugins.availe.kmp.targets)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.shared.logic)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}