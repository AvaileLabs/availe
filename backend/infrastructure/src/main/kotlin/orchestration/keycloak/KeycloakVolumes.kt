package com.availelabs.orchestration.keycloak

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface KeycloakVolumes : ContainerRunSpecification.VolumeMount {
    data object Data : KeycloakVolumes {
        override val hostSource = "availe-keycloak-data"
        override val containerPath = "/opt/keycloak/data"
        override val isBindMount = false
    }

    data class ProviderJar(val hostFilePath: String) : KeycloakVolumes {
        override val hostSource = hostFilePath
        override val containerPath = "/opt/keycloak/providers/jid-listener.jar"
        override val isBindMount = true
    }
}

internal class KeycloakVolumesList(
    spiJarHostPath: String
) : ContainerRunSpecification.VolumeList {
    val data = KeycloakVolumes.Data
    val providerJar = KeycloakVolumes.ProviderJar(spiJarHostPath)

    override val all = listOf(data, providerJar)
}