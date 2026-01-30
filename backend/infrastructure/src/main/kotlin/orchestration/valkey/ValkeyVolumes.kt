package com.availelabs.orchestration.valkey

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface ValkeyVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : ValkeyVolumes {
        override val hostSource = "availe-valkey-data"
        override val containerPath = "/data"
        override val isBindMount = false
    }
}

internal object ValkeyVolumesList : ContainerRunSpecification.VolumeList {
    val Data = ValkeyVolumes.Data

    override val all = listOf(Data)
}