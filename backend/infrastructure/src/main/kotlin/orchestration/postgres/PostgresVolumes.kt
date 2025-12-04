package com.availelabs.orchestration.postgres

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : PostgresVolumes {
        override val hostSource = "availe-postgres-data"
        override val containerPath = "/var/lib/postgresql"
        override val isBindMount = false
    }
}

internal object PostgresVolumesList : ContainerRunSpecification.VolumeList {
    val Data = PostgresVolumes.Data
    override val all = listOf(Data)
}