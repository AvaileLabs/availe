plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.availelabs"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.docker.java.bom))
    implementation(libs.docker.java)
    implementation(libs.docker.java.transport.httpclient5)
    implementation(libs.keycloak.admin.client)
    implementation(libs.postgresql)
    implementation(libs.logback)
    implementation(project(":shared"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

val containersSetup by tasks.registering(JavaExec::class) {
    group = "infrastructure"
    description = "Ensures Podman/Docker containers are setup for the application."
    mainClass.set("com.availelabs.MainKt")
    classpath = sourceSets.main.get().runtimeClasspath
}

val destroyAll by tasks.registering(JavaExec::class) {
    group = "infrastructure"
    description = "Stops and removes all containers, volumes, and networks for the application."
    mainClass.set("com.availelabs.MainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    args("--destroyAll")
}