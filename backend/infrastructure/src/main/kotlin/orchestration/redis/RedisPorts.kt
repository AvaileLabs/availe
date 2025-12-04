package com.availelabs.orchestration.redis

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface IRedisPorts : ContainerRunSpecification.PortMapping {
    data object Redis : IRedisPorts {
        override val hostPortNumber = SharedNetworkConstants.REDIS_PUBLIC_PORT
        override val containerPortNumber = 6379
    }
}

internal object RedisPortsList : ContainerRunSpecification.PortList {
    val Redis = IRedisPorts.Redis
    override val all = listOf(Redis)
}