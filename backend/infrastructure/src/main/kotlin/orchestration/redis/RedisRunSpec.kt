package com.availelabs.orchestration.redis

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun redisRunSpec(): ContainerRunSpecification<RedisPortsList, RedisVolumesList> {
    return ContainerRunSpecification(
        name = SharedNetworkConstants.CONTAINER_HOSTNAME_REDIS,
        image = "docker.io/library/redis",
        tag = "8.4",
        portMappings = RedisPortsList,
        volumes = RedisVolumesList
    )
}