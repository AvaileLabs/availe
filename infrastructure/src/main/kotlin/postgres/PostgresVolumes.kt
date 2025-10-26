package com.availelabs.postgres

import com.availelabs.containers.domain.ContainerRunSpecification

internal sealed interface PostgresVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : PostgresVolumes {
        override val volumeName = "availe-postgres-data"
        override val containerPath = "/var/lib/postgresql"
    }
}

internal object PostgresVolumesList : ContainerRunSpecification.VolumeList {
    val Data = PostgresVolumes.Data
    override val all = listOf(Data)
}