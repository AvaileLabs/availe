plugins {
    id("availe.kmp.android")
}

kotlin {
    sourceSets {
        val commonMain by getting

        val androidMain by getting
        val jvmMain by getting
        val iosMain by getting
        val webMain by getting

        val nonAndroidMain by creating {
            dependsOn(commonMain)
        }

        val nonWebMain by creating {
            dependsOn(commonMain)
        }

        jvmMain.dependsOn(nonAndroidMain)
        iosMain.dependsOn(nonAndroidMain)
        webMain.dependsOn(nonAndroidMain)

        androidMain.dependsOn(nonWebMain)
        jvmMain.dependsOn(nonWebMain)
        iosMain.dependsOn(nonWebMain)
    }
}