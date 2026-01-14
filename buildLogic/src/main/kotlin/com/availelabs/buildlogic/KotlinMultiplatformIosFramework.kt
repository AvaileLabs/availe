package com.availelabs.buildlogic

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

fun KotlinMultiplatformExtension.configureIosFramework(
    baseName: String = "ComposeApp",
    isStatic: Boolean = true,
    configure: Framework.() -> Unit = {},
) {
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        if (konanTarget.family != Family.IOS) return@configureEach

        binaries.framework {
            this.baseName = baseName
            this.isStatic = isStatic
            configure()
        }
    }
}