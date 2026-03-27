package com.availelabs.orchestration.postgresejabberd

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun postgresEjabberdRunSpecification(): ContainerRunSpecification<PostgresEjabberdPortsList, PostgresEjabberdVolumesList> {
    val postgresEjabberdConfiguration = loadPostgresEjabberdConfig()

    val environmentVariables = listOf(
        "POSTGRES_DB=${postgresEjabberdConfiguration.databaseName}",
        "POSTGRES_USER=${postgresEjabberdConfiguration.databaseUser}",
        "POSTGRES_PASSWORD=${postgresEjabberdConfiguration.databasePassword}",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.POSTGRES_EJABBERD_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.POSTGRES_REPOSITORY,
        tag = InfrastructureRegistry.Images.POSTGRES_TAG,
        environmentVariables = environmentVariables,
        portMappings = PostgresEjabberdPortsList,
        volumes = PostgresEjabberdVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}