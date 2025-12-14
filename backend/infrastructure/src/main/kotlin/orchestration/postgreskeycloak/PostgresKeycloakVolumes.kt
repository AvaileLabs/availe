package com.availelabs.orchestration.postgreskeycloak

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresKeycloakVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : PostgresKeycloakVolumes {
        override val hostSource = "availe-postgres-keycloak-data"
        override val containerPath = "/var/lib/postgresql"
        override val isBindMount = false
    }
}

internal object PostgresKeycloakVolumesList : ContainerRunSpecification.VolumeList {
    val Data = PostgresKeycloakVolumes.Data
    override val all = listOf(Data)
}