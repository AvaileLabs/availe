package com.availelabs.keycloak

import com.availelabs.containers.domain.ContainerRunSpecification

internal sealed interface KeycloakPorts : ContainerRunSpecification.PortMapping {
    data object Http : KeycloakPorts {
        override val hostPortNumber = 8180
        override val containerPortNumber = 8080
    }

    data object Management : KeycloakPorts {
        override val hostPortNumber = 9001
        override val containerPortNumber = 9000
    }
}

internal object KeycloakPortsList : ContainerRunSpecification.PortList {
    val Http = KeycloakPorts.Http
    val Management = KeycloakPorts.Management

    override val all = listOf(Http, Management)
}