plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.gradle.buildconfig)
}

group = "com.availelabs"
version = "1.0.0"

// 1. Define a configuration to safely resolve the artifact from the other project
val keycloakSpiArtifact by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(platform(libs.docker.java.bom))
    implementation(libs.docker.java)
    implementation(libs.docker.java.transport.httpclient5)
    implementation(libs.keycloak.admin.client)
    implementation(libs.postgresql)
    implementation(libs.logback)
    implementation(platform(libs.bouncycastle.bom))
    implementation(libs.bouncycastle.pkix)
    implementation(libs.bouncycastle.prov)
    implementation(projects.shared.logic)
    implementation(projects.shared.configuration)
    testImplementation(kotlin("test"))

    // 2. Add the dependency. isTransitive = false ensures we only get the project jar, not its dependencies.
    keycloakSpiArtifact(project(":backend:keycloakSpi")) {
        isTransitive = false
    }
}

tasks.test {
    useJUnitPlatform()
}

// 3. Create a provider that resolves the configuration to a file path string lazily
val keycloakSpiJarPathProvider = providers.provider {
    val jarFile = keycloakSpiArtifact.singleFile
    "\"${jarFile.absolutePath}\""
}

buildConfig {
    packageName("com.availelabs.infrastructure.generated")
    className("InfrastructureConstants")

    useKotlinOutput {
        internalVisibility = true
    }

    sourceSets.named("main") {
        buildConfigField("String", "KEYCLOAK_SPI_JAR_PATH", keycloakSpiJarPathProvider)
    }
}

val containersSetup by tasks.registering(JavaExec::class) {
    group = "infrastructure"
    description = "Ensures Podman/Docker containers are setup for the application."
    mainClass.set("com.availelabs.MainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")

    // 4. Ensure the jar is actually built before we try to run the infrastructure setup
    dependsOn(keycloakSpiArtifact)
}

val destroyAll by tasks.registering(JavaExec::class) {
    group = "infrastructure"
    description = "Stops and removes all containers, volumes, and networks for the application."
    mainClass.set("com.availelabs.MainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    args("--destroyAll")
}