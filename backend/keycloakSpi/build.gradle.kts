import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.availelabs"
version = "1.0.0"

dependencies {
    compileOnly(libs.keycloak.server.spi)
    compileOnly(libs.keycloak.server.spi.private)
    compileOnly(libs.keycloak.services)
    implementation(libs.kotlin.stdlib)
    implementation(projects.shared.logic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.withType<Test> {
    useJUnitPlatform()
}