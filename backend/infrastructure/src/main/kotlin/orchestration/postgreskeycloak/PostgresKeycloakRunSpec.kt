package com.availelabs.orchestration.postgreskeycloak

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun postgresKeycloakRunSpecification(): ContainerRunSpecification<PostgresKeycloakPortsList, PostgresKeycloakVolumesList> {
    val postgresKeycloakConfiguration = loadPostgresKeycloakConfig()

    val environmentVariables = listOf(
        "POSTGRES_DB=${postgresKeycloakConfiguration.databaseName}",
        "POSTGRES_USER=${postgresKeycloakConfiguration.databaseUser}",
        "POSTGRES_PASSWORD=${postgresKeycloakConfiguration.databasePassword}",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.POSTGRES_KEYCLOAK_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.POSTGRES_REPOSITORY,
        tag = InfrastructureRegistry.Images.POSTGRES_TAG,
        environmentVariables = environmentVariables,
        portMappings = PostgresKeycloakPortsList,
        volumes = PostgresKeycloakVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}