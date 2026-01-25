plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.availelabs"
version = "1.0.0"

dependencies {
    compileOnly("org.keycloak:keycloak-server-spi:26.0.7")
    compileOnly("org.keycloak:keycloak-server-spi-private:26.0.7")
    compileOnly("org.keycloak:keycloak-services:26.0.7")
    compileOnly(libs.kotlin.stdlib)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}