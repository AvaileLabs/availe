package com.availelabs.orchestration.postgreskeycloak

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.utils.ServiceWaiter

internal fun postgresKeycloakRunSpecification(
    serviceWaiter: ServiceWaiter,
    postgresKeycloakConfig: PostgresKeycloakConfig
): ContainerRunSpecification<PostgresKeycloakPortsList, PostgresKeycloakVolumesList> {
    val environmentVariables = listOf(
        "POSTGRES_DB=${postgresKeycloakConfig.databaseName}",
        "POSTGRES_USER=${postgresKeycloakConfig.databaseUser}",
        "POSTGRES_PASSWORD=${postgresKeycloakConfig.databasePassword}",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.POSTGRES_KEYCLOAK_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.POSTGRES_REPOSITORY,
        tag = InfrastructureRegistry.Images.POSTGRES_TAG,
        environmentVariables = environmentVariables,
        portMappings = PostgresKeycloakPortsList,
        volumes = PostgresKeycloakVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = true,
        containerHealthCheckVerificationAction = { serviceWaiter.waitForPostgresKeycloak(postgresKeycloakConfig) }
    )
}