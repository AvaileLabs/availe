import com.github.gradle.node.npm.task.NpxTask

plugins {
    id("base")
    alias(libs.plugins.gradle.node)
}

group = "com.availelabs"
version = "1.0.0"

node {
    download.set(true)
    version.set("25.2.1")
    workDir.set(layout.projectDirectory.dir(".gradle/nodejs"))
    npmWorkDir.set(layout.projectDirectory.dir(".gradle/npm"))
    nodeProjectDir.set(layout.projectDirectory)
}

val enableCorepack by tasks.registering(NpxTask::class) {
    command.set("corepack")
    args.set(listOf("enable"))
    dependsOn(tasks.named("nodeSetup"))
}

tasks.named("pnpmSetup") {
    enabled = false
    dependsOn(enableCorepack)
}

tasks.named("pnpmInstall") {
    dependsOn(enableCorepack)
}

val updateKeycloakThemeDependencies by tasks.registering(NpxTask::class) {
    group = "dependencies"
    description = "Updates pnpm dependencies for the Keycloak theme"
    dependsOn(enableCorepack)

    command.set("pnpm")
    args.set(listOf("update"))

    inputs.file("package.json")
    outputs.file("pnpm-lock.yaml")
    outputs.dir("node_modules")
}

val buildKeycloakTheme by tasks.registering(NpxTask::class) {
    group = "build"
    description = "Builds the Keycloak theme JAR using pnpm and Keycloakify"
    dependsOn(tasks.named("pnpmInstall"))

    command.set("pnpm")
    args.set(listOf("build"))

    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("pnpm-lock.yaml")
    inputs.file("vite.config.ts")
    inputs.file("tsconfig.json")

    outputs.dir("dist_keycloak")
}

tasks.named("assemble") {
    dependsOn(buildKeycloakTheme)
}

tasks.named("clean") {
    doLast {
        delete(layout.projectDirectory.dir("dist_keycloak"))
        delete(layout.projectDirectory.dir("dist"))
    }
}