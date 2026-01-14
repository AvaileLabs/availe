plugins {
    id("availe.kmp.android")
}

kotlin {
    sourceSets {
        val commonMain by getting

        val androidMain by getting
        val jvmMain by getting
        val iosMain by getting

        val nonWebMain by creating {
            dependsOn(commonMain)
        }

        androidMain.dependsOn(nonWebMain)
        jvmMain.dependsOn(nonWebMain)
        iosMain.dependsOn(nonWebMain)
    }
}