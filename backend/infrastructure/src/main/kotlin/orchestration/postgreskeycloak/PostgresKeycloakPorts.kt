package com.availelabs.orchestration.postgreskeycloak

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresKeycloakPorts : ContainerRunSpecification.PortMapping {
    data object Postgres : PostgresKeycloakPorts {
        override val hostPortNumber = SharedNetworkConstants.KEYCLOAK_POSTGRES_PORT
        override val containerPortNumber = 5432
    }
}

internal object PostgresKeycloakPortsList : ContainerRunSpecification.PortList {
    val Postgres = PostgresKeycloakPorts.Postgres

    override val all = listOf(Postgres)
}