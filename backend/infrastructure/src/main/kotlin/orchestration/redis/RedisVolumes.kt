package com.availelabs.orchestration.redis

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface IRedisVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : IRedisVolumes {
        override val volumeName = "availe-redis-data"
        override val containerPath = "/data"
    }
}

internal object RedisVolumesList : ContainerRunSpecification.VolumeList {
    val Data = IRedisVolumes.Data
    override val all = listOf(Data)
}