import com.availelabs.buildlogic.getHostIpAddress

plugins {
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val useLocalIpForDebug = providers.gradleProperty("availe.dev.useLocalIp").getOrElse("false").toBoolean()

kotlin {
    jvmToolchain(17)
}

abstract class GenerateNetworkSecurityConfig : DefaultTask() {

    @get:InputFile
    abstract val template: RegularFileProperty

    @get:Input
    abstract val hostIp: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val xml = template.get().asFile.readText()
            .replace($$"${HOST_IP}", hostIp.get())

        val outFile = outputDir.get().file("xml/network_security_config.xml").asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(xml)
    }
}

val generateDebugNetworkSecurityConfig =
    tasks.register<GenerateNetworkSecurityConfig>("generateDebugNetworkSecurityConfig") {
        template.set(layout.projectDirectory.file("src/main/res-templates/network_security_config.xml.tpl"))
        hostIp.set(providers.provider { getHostIpAddress() })
        outputDir.set(layout.buildDirectory.dir("generated/networkSecurity/debug/res"))
    }

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        if (useLocalIpForDebug) {
            variant.sources.res?.addGeneratedSourceDirectory(
                generateDebugNetworkSecurityConfig,
                GenerateNetworkSecurityConfig::outputDir
            )
        }
    }
}

android {
    namespace = "com.availelabs.availe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.availelabs.availe"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        addManifestPlaceholders(
            mapOf("oidcRedirectScheme" to "com.availe.auth")
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(projects.client.composeApp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.oidc.appsupport)
    implementation(libs.oidc.ktor)
    debugImplementation(libs.androidx.ui.tooling)
}