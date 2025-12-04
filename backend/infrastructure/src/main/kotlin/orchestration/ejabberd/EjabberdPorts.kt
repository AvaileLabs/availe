package com.availelabs.orchestration.ejabberd

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface EjabberdPorts : ContainerRunSpecification.PortMapping {
    data object Client : EjabberdPorts {
        override val hostPortNumber = SharedNetworkConstants.EJABBERD_CLIENT_PORT
        override val containerPortNumber = 5222
    }

    data object Admin : EjabberdPorts {
        override val hostPortNumber = SharedNetworkConstants.EJABBERD_ADMIN_PORT
        override val containerPortNumber = 5280
    }
}

internal object EjabberdPortsList : ContainerRunSpecification.PortList {
    val Client = EjabberdPorts.Client
    val Admin = EjabberdPorts.Admin

    override val all = listOf(Client, Admin)
}