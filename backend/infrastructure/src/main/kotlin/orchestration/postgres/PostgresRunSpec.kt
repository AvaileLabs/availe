package com.availelabs.orchestration.postgres

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun postgresRunSpec(
    postgresConfig: PostgresConfig
): ContainerRunSpecification<PostgresPortsList, PostgresVolumesList> {
    val databaseName = postgresConfig.dbUrl.substringAfterLast('/')

    val environmentVariables = listOf(
        "POSTGRES_DB=$databaseName",
        "POSTGRES_USER=${postgresConfig.dbUser}",
        "POSTGRES_PASSWORD=${postgresConfig.dbPassword}",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.POSTGRES_MAIN_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.POSTGRES_REPOSITORY,
        tag = InfrastructureRegistry.Images.POSTGRES_TAG,
        environmentVariables = environmentVariables,
        portMappings = PostgresPortsList,
        volumes = PostgresVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = true
    )
}