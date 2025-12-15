import com.github.gradle.node.npm.task.NpxTask

plugins {
    id("base")
    alias(libs.plugins.gradle.node)
}

group = "com.availelabs"
version = "1.0.0"

node {
    download.set(true)
    version.set("22.11.0")
    workDir.set(layout.projectDirectory.dir(".gradle/nodejs"))
    npmWorkDir.set(layout.projectDirectory.dir(".gradle/npm"))
    nodeProjectDir.set(layout.projectDirectory)
}

val enableCorepack by tasks.registering(NpxTask::class) {
    command.set("corepack")
    args.set(listOf("enable"))
    dependsOn(tasks.named("nodeSetup"))
}

val installDependencies by tasks.registering(NpxTask::class) {
    dependsOn(enableCorepack)
    command.set("pnpm")
    args.set(listOf("install"))
    inputs.file("package.json")
    inputs.file("pnpm-lock.yaml")
    outputs.dir("node_modules")
}

val updateDependencies by tasks.registering(NpxTask::class) {
    dependsOn(enableCorepack)
    command.set("pnpm")
    args.set(listOf("update"))
    inputs.file("package.json")
    outputs.file("pnpm-lock.yaml")
    outputs.dir("node_modules")
}

val buildTheme by tasks.registering(NpxTask::class) {
    dependsOn(installDependencies)
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
    dependsOn(buildTheme)
}

tasks.named("clean") {
    doLast {
        delete(layout.projectDirectory.dir("dist_keycloak"))
        delete(layout.projectDirectory.dir("dist"))
    }
}