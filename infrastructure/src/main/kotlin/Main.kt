package com.availelabs

import com.availelabs.containers.ContainerOrchestrator
import com.availelabs.containers.core.*

internal fun main() {
    val dockerNetworkName = "availe-network"

    val dockerClient = DockerClientProvider().createDockerClient()
    val dockerNetworkManager = DockerNetworkManager(dockerClient, dockerNetworkName)
    val dockerVolumeManager = DockerVolumeManager(dockerClient)
    val dockerImageManager = DockerImageManager(dockerClient)
    val dockerContainerManager = DockerContainerManager(dockerClient, dockerNetworkName)

    val containerOrchestrator = ContainerOrchestrator(
        dockerNetworkManager = dockerNetworkManager,
        dockerVolumeManager = dockerVolumeManager,
        dockerImageManager = dockerImageManager,
        dockerContainerManager = dockerContainerManager
    )
}