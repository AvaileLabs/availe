plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.availelabs"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.docker.java.bom))
    implementation(libs.docker.java)
    implementation(libs.docker.java.transport.httpclient5)
    implementation(libs.keycloak.admin.client)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}