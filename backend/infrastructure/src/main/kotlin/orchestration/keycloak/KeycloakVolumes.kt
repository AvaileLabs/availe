package com.availelabs.orchestration.keycloak

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface KeycloakVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : KeycloakVolumes {
        override val volumeName = "availe-keycloak-data"
        override val containerPath = "/opt/keycloak/data"
    }
}

internal object KeycloakVolumesList : ContainerRunSpecification.VolumeList {
    val Data = KeycloakVolumes.Data
    override val all = listOf(Data)
}