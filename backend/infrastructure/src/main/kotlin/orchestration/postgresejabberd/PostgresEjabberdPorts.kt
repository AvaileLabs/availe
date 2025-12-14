package com.availelabs.orchestration.postgresejabberd

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresEjabberdPorts : ContainerRunSpecification.PortMapping {
    data object Postgres : PostgresEjabberdPorts {
        override val hostPortNumber = SharedNetworkConstants.EJABBERD_POSTGRES_PORT
        override val containerPortNumber = 5432
    }
}

internal object PostgresEjabberdPortsList : ContainerRunSpecification.PortList {
    val Postgres = PostgresEjabberdPorts.Postgres

    override val all = listOf(Postgres)
}