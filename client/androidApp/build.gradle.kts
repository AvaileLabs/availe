import com.availelabs.buildlogic.HostIpValueSource

plugins {
    alias(libs.plugins.availe.buildutils)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val useLocalIpAddressForDebug = providers.gradleProperty("availe.dev.useLocalIp").getOrElse("false").toBoolean()
val openIdConnectRedirectScheme = "com.availe.auth"

abstract class GenerateNetworkSecurityConfig : DefaultTask() {

    @get:InputFile
    abstract val templateFile: RegularFileProperty

    @get:Input
    abstract val hostIpAddress: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val xmlContent = templateFile.get().asFile.readText()
            .replace("\${HOST_IP}", hostIpAddress.get())

        val outputFile = outputDirectory.get().file("xml/network_security_config.xml").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(xmlContent)
    }
}

val generateDebugNetworkSecurityConfig =
    tasks.register<GenerateNetworkSecurityConfig>("generateDebugNetworkSecurityConfig") {
        templateFile.set(layout.projectDirectory.file("src/main/res-templates/network_security_config.xml.tpl"))
        hostIpAddress.set(providers.of(HostIpValueSource::class) {})
        outputDirectory.set(layout.buildDirectory.dir("generated/networkSecurity/debug/res"))
    }

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        if (useLocalIpAddressForDebug) {
            variant.sources.res?.addGeneratedSourceDirectory(
                generateDebugNetworkSecurityConfig,
                GenerateNetworkSecurityConfig::outputDirectory
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
            mapOf("oidcRedirectScheme" to openIdConnectRedirectScheme)
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
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.ktor.bom))
    implementation(platform(libs.coil.bom))
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