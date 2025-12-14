package com.availelabs.orchestration.postgresejabberd

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface PostgresEjabberdVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : PostgresEjabberdVolumes {
        override val hostSource = "availe-postgres-ejabberd-data"
        override val containerPath = "/var/lib/postgresql"
        override val isBindMount = false
    }
}

internal object PostgresEjabberdVolumesList : ContainerRunSpecification.VolumeList {
    val Data = PostgresEjabberdVolumes.Data
    override val all = listOf(Data)
}