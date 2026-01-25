package com.availelabs.orchestration.keycloak

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.postgreskeycloak.loadPostgresKeycloakConfig

internal fun keycloakRunSpec(): ContainerRunSpecification<KeycloakPortsList, KeycloakVolumesList> {
    val keycloakConfig = loadKeycloakConfig()
    val postgresKeycloakConfig = loadPostgresKeycloakConfig()

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
        name = "availe-keycloak",
        image = "quay.io/keycloak/keycloak",
        tag = "26.0.7",
        environmentVariables = environmentVariables,
        portMappings = KeycloakPortsList,
        volumes = KeycloakVolumesList,
        command = listOf("start-dev", "--features=scripts")
    )
}