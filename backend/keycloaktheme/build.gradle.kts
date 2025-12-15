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

val buildTheme by tasks.registering(NpxTask::class) {
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
    dependsOn(buildTheme)
}

tasks.named("clean") {
    doLast {
        delete(layout.projectDirectory.dir("dist_keycloak"))
        delete(layout.projectDirectory.dir("dist"))
    }
}