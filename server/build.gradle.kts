plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "com.availelabs.availe"
version = "1.0.0"
application {
    mainClass.set("com.availelabs.availe.ApplicationKt")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    implementation(platform(libs.http4k.bom))
    implementation(libs.http4k.core)
    implementation(libs.http4k.server.helidon)
    implementation(libs.http4k.api.openapi)
    implementation(libs.http4k.format.kotlinx.serialization)

    testImplementation(libs.kotlin.testJunit)
}