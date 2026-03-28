package com.availelabs.orchestration.keycloak

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.infrastructure.generated.InfrastructureConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.postgreskeycloak.PostgresKeycloakConfig
import java.io.File

internal fun keycloakRunSpec(
    keycloakConfig: KeycloakConfig,
    postgresKeycloakConfig: PostgresKeycloakConfig
): ContainerRunSpecification<KeycloakPortsList, KeycloakVolumesList> {
    val spiJarFile = File(InfrastructureConstants.KEYCLOAK_SPI_JAR_PATH)

    check(spiJarFile.exists()) {
        "Keycloak SPI JAR not found at: ${spiJarFile.absolutePath}. " +
                "Please run './gradlew :backend:keycloakSpi:jar' to build it."
    }

    val environmentVariables = listOf(
        "KEYCLOAK_ADMIN=${keycloakConfig.adminUser}",
        "KEYCLOAK_ADMIN_PASSWORD=${keycloakConfig.adminPassword}",
        "KC_DB=postgres",
        "KC_DB_URL_HOST=${SharedNetworkConstants.KEYCLOAK_DB_HOST}",
        "KC_DB_URL_DATABASE=${postgresKeycloakConfig.databaseName}",
        "KC_DB_USERNAME=${postgresKeycloakConfig.databaseUser}",
        "KC_DB_PASSWORD=${postgresKeycloakConfig.databasePassword}",
        "KC_DB_SCHEMA=${SharedNetworkConstants.DATABASE_SCHEMA_KEYCLOAK}",
        "KC_HOSTNAME=${NetworkConstants.HOST_IP}",
        "KC_HEALTH_ENABLED=true",
        "KC_HTTP_PORT=${KeycloakPortsList.Http.containerPortNumber}",
        "KC_HTTPS_PORT=8443",
        "KC_MANAGEMENT_HTTP_PORT=${KeycloakPortsList.Management.containerPortNumber}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.KEYCLOAK_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.KEYCLOAK_REPOSITORY,
        tag = InfrastructureRegistry.Images.KEYCLOAK_TAG,
        environmentVariables = environmentVariables,
        portMappings = KeycloakPortsList,
        volumes = KeycloakVolumesList(spiJarHostPath = spiJarFile.absolutePath),
        command = listOf("start-dev", "--features=scripts"),
        requiresDataBackup = false
    )
}