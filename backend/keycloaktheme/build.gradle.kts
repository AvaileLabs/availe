import com.github.gradle.node.npm.task.NpxTask

plugins {
    id("base")
    alias(libs.plugins.gradle.node)
}

group = "com.availelabs"
version = "1.0.0"

node {
    download.set(true)
    // Recommend using LTS (v22) instead of bleeding edge (v25) for better stability
    version.set("22.12.0")
    // Tell the plugin to manage pnpm for you
    pnpmVersion.set("9.15.0")

    workDir.set(layout.projectDirectory.dir(".gradle/nodejs"))
    npmWorkDir.set(layout.projectDirectory.dir(".gradle/npm"))
    // pnpmWorkDir will default to .gradle/pnpm which is where the plugin looks
    nodeProjectDir.set(layout.projectDirectory)
}

// REMOVED: val enableCorepack ...
// REMOVED: tasks.named("pnpmSetup") ...

// Ensure pnpm is installed before we try to use it
tasks.named("pnpmInstall") {
    dependsOn(tasks.named("pnpmSetup"))
}

val updateKeycloakThemeDependencies by tasks.registering(NpxTask::class) {
    group = "dependencies"
    description = "Updates pnpm dependencies for the Keycloak theme"
    // REMOVED: dependsOn(enableCorepack)
    dependsOn(tasks.named("pnpmSetup")) // Ensure pnpm is available

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