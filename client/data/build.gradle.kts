import app.cash.sqldelight.gradle.SqlDelightDatabase
import com.android.build.api.dsl.androidLibrary
import com.availelabs.buildlogic.configureIosFramework
import java.security.MessageDigest

plugins {
    alias(libs.plugins.availe.kmp.android)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.gradle.buildconfig)
}

group = "com.availelabs"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "com.availelabs.availe"
    }

    configureIosFramework()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.logic)
            implementation(projects.client.core)
            implementation(projects.client.xmpp)
            implementation(projects.shared.configuration)
            implementation(projects.client.domain)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(libs.store5)
            implementation(libs.xmlutil)

            implementation(libs.oidc.appsupport)
            implementation(libs.oidc.ktor)
            implementation(libs.oidc.preferences)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            implementation(libs.decompose.extensions.compose.experimental)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.utils)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        webMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlin.browser)

            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", libs.versions.sqljs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
        }

        jsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
        }

        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver.wasm.js)
        }
    }
}

sqldelight {
    databases {
        create("ClientDatabase", Action<SqlDelightDatabase> {
            packageName.set("com.availelabs.availe.db")
            generateAsync.set(true)
        })
    }
}

val databaseSchemaHashProvider: Provider<String> = providers.provider {
    val messageDigest = MessageDigest.getInstance("MD5")
    val schemaDirectory = layout.projectDirectory.dir("src/commonMain/sqldelight").asFile

    if (schemaDirectory.exists()) {
        fileTree(schemaDirectory) {
            include("**/*.sq")
        }.files.sortedBy { file -> file.absolutePath }
            .forEach { file ->
                messageDigest.update(file.readBytes())
            }
        val hash = messageDigest.digest().joinToString("") { byte -> "%02x".format(byte) }
        "\"$hash\""
    } else {
        "\"empty\""
    }
}

buildConfig {
    packageName("com.availelabs.availe.database")
    className("DatabaseBuildConfig")

    useKotlinOutput {
        internalVisibility = true
    }

    buildConfigField("String", "SCHEMA_HASH", databaseSchemaHashProvider)
}