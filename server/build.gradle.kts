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

    implementation(libs.kotlinx.serialization.json)
    implementation("javax.money:money-api:1.1")
    implementation("org.javamoney.moneta:moneta-core:1.4.5")
    implementation("org.javamoney.moneta:moneta-convert-ecb:1.4.5")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


//    testImplementation(libs.kotlin.testJunit)
}