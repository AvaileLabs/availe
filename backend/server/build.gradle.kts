import com.google.devtools.ksp.gradle.KspAATask
import org.flywaydb.gradle.task.AbstractFlywayTask

buildscript {
    dependencies {
        classpath(libs.flyway.database.postgresql)
    }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.jooq.codegen)
    alias(libs.plugins.flyway)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kreplica)
    application
}

val dbUrl = providers.gradleProperty("db.url")
val dbUser = providers.gradleProperty("db.user")
val dbPassword = providers.gradleProperty("db.password")

group = "com.availelabs.availe"
version = "1.0.0"
application {
    mainClass.set("com.availelabs.availe.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared.logic)
    implementation(projects.shared.configuration)
    implementation(libs.logback)

    implementation(libs.kotlin.reflect)

    implementation(platform(libs.http4k.bom))
    implementation(libs.http4k.core)
    implementation(libs.http4k.server.helidon)
    implementation(libs.http4k.api.openapi)
    implementation(libs.http4k.format.kotlinx.serialization)
    implementation(libs.http4k.format.jackson)
    implementation(libs.http4k.security.oauth)

    implementation(libs.java.jwt)
    implementation(libs.jwks.rsa)
    implementation(libs.jedis)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.money.api)
    implementation(libs.moneta.core)
    implementation(libs.moneta.convert.ecb)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    // database dependencies
    implementation(libs.jooq)
    implementation(libs.hikariCP)
    jooqCodegen(libs.postgresql)
    implementation(libs.postgresql)
    implementation(libs.flyway.database.postgresql)
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = dbUrl.get()
            user = dbUser.get()
            password = dbPassword.get()
        }
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "availe"
                excludes = "flyway_schema_history"
            }
            target {
                packageName = "com.example.jooq"
            }
        }
    }
}

flyway {
    url = dbUrl.get()
    user = dbUser.get()
    password = dbPassword.get()
    schemas = arrayOf("availe")
    createSchemas = true
}

tasks.compileKotlin {
    dependsOn(tasks.jooqCodegen)
}

tasks.jooqCodegen {
    dependsOn(tasks.flywayMigrate)
    inputs.files(fileTree("src/main/resources/db/migration"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("jdbc.url", dbUrl)
    inputs.property("jdbc.user", dbUser)
}

tasks.withType<KspAATask> {
    dependsOn(tasks.jooqCodegen)
}

tasks.withType<AbstractFlywayTask> {
    notCompatibleWithConfigurationCache("because https://github.com/flyway/flyway/issues/3550")
}

tasks.withType<Test> {
    useJUnitPlatform()
}