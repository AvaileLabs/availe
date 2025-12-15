package com.availelabs.orchestration.postgresejabberd

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
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
        name = SharedNetworkConstants.EJABBERD_DB_HOST,
        image = "docker.io/library/postgres",
        tag = "18",
        environmentVariables = environmentVariables,
        portMappings = PostgresEjabberdPortsList,
        volumes = PostgresEjabberdVolumesList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}