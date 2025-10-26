package com.availelabs.containers.domain

import com.github.dockerjava.api.model.RestartPolicy

internal data class ContainerRunSpecification<P : ContainerRunSpecification.PortList, V : ContainerRunSpecification.VolumeList>(
    val name: String,
    val image: String,
    val tag: String,
    val environmentVariables: List<String> = emptyList(),
    val portMappings: P,
    val volumes: V,
    val restartPolicy: RestartPolicy = RestartPolicy.unlessStoppedRestart(),
    val command: List<String> = emptyList()
) {
    interface VolumeMount {
        val volumeName: String
        val containerPath: String
    }

    interface VolumeList {
        val all: List<VolumeMount>
    }

    interface PortMapping {
        val hostPortNumber: Int
        val containerPortNumber: Int
    }

    interface PortList {
        val all: List<PortMapping>
    }
}