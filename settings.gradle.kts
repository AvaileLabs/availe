rootProject.name = "availe"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("buildLogic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":client:composeApp")
include(":client:androidApp")
include(":client:domain")
include(":client:data")
include(":client:core")
include(":client:designSystem")
include(":client:uiComponents")
include("client:xmpp")
include(":client:features:appShell")
include(":client:features:session")
include(":client:features:auth")
include(":client:features:home")
include(":client:features:listing")
include(":client:features:profile")
include(":client:features:chat")

include(":backend:server")
include(":backend:infrastructure")
include("backend:keycloakSpi")

include(":shared:logic")
include(":shared:configuration")