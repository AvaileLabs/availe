package com.availelabs.orchestration.postgresejabberd

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.utils.ServiceWaiter

internal fun postgresEjabberdRunSpecification(
    serviceWaiter: ServiceWaiter,
    postgresEjabberdConfig: PostgresEjabberdConfig
): ContainerRunSpecification<PostgresEjabberdPortsList, PostgresEjabberdVolumesList> {
    val environmentVariables = listOf(
        "POSTGRES_DB=${postgresEjabberdConfig.databaseName}",
        "POSTGRES_USER=${postgresEjabberdConfig.databaseUser}",
        "POSTGRES_PASSWORD=${postgresEjabberdConfig.databasePassword}",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.POSTGRES_EJABBERD_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.POSTGRES_REPOSITORY,
        tag = InfrastructureRegistry.Images.POSTGRES_TAG,
        environmentVariables = environmentVariables,
        portMappings = PostgresEjabberdPortsList,
        volumes = PostgresEjabberdVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = true,
        containerHealthCheckVerificationAction = { serviceWaiter.waitForPostgresEjabberd(postgresEjabberdConfig) }
    )
}