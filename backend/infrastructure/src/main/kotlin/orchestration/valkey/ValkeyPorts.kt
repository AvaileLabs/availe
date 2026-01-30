package com.availelabs.orchestration.valkey

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface ValkeyPorts : ContainerRunSpecification.PortMapping {
    data object Primary : ValkeyPorts {
        override val hostPortNumber = SharedNetworkConstants.VALKEY_PORT
        override val containerPortNumber = 6379
    }
}

internal object ValkeyPortsList : ContainerRunSpecification.PortList {
    val Primary = ValkeyPorts.Primary

    override val all = listOf(Primary)
}