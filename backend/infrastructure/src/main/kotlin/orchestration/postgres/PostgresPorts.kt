package com.availelabs.orchestration.postgres

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresPorts : ContainerRunSpecification.PortMapping {
    data object Postgres : PostgresPorts {
        override val hostPortNumber = SharedNetworkConstants.POSTGRES_PORT
        override val containerPortNumber = 5432
    }
}

internal object PostgresPortsList : ContainerRunSpecification.PortList {
    val Postgres = PostgresPorts.Postgres
    override val all = listOf(Postgres)
}