package com.availelabs.orchestration.containers.domain

import com.github.dockerjava.api.model.RestartPolicy

internal data class ContainerRunSpecification<P : ContainerRunSpecification.PortList, V : ContainerRunSpecification.VolumeList>(
    val name: String,
    val image: String,
    val tag: String,
    val environmentVariables: List<String> = emptyList(),
    val portMappings: P,
    val volumes: V,
    val ulimits: List<Ulimit> = emptyList(),
    val restartPolicy: RestartPolicy = RestartPolicy.unlessStoppedRestart(),
    val command: List<String> = emptyList(),
    val logStrategy: LogStrategy = LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
    val requiresDataBackup: Boolean = false
) {
    enum class LogStrategy {
        BUFFER_AND_DUMP_ON_FAILURE,
        STREAM_ALWAYS,
        SILENT
    }

    data class Ulimit(
        val name: String,
        val softLimit: Long,
        val hardLimit: Long
    )

    interface VolumeMount {
        val hostSource: String
        val containerPath: String
        val isBindMount: Boolean
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