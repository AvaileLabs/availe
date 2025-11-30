package com.availelabs.orchestration.keycloak

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface KeycloakPorts : ContainerRunSpecification.PortMapping {
    data object Http : KeycloakPorts {
        override val hostPortNumber = SharedNetworkConstants.KEYCLOAK_PUBLIC_PORT
        override val containerPortNumber = 8080
    }

    data object Management : KeycloakPorts {
        override val hostPortNumber = SharedNetworkConstants.KEYCLOAK_MANAGEMENT_PORT
        override val containerPortNumber = 9000
    }
}

internal object KeycloakPortsList : ContainerRunSpecification.PortList {
    val Http = KeycloakPorts.Http
    val Management = KeycloakPorts.Management

    override val all = listOf(Http, Management)
}