package com.availelabs.orchestration.keycloak

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface KeycloakVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : KeycloakVolumes {
        override val hostSource = "availe-keycloak-data"
        override val containerPath = "/opt/keycloak/data"
        override val isBindMount = false
    }
}

internal object KeycloakVolumesList : ContainerRunSpecification.VolumeList {
    val Data = KeycloakVolumes.Data
    override val all = listOf(Data)
}