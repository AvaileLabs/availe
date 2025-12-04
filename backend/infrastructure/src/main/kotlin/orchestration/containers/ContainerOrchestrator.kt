package com.availelabs.orchestration.containers

import com.availelabs.orchestration.containers.core.DockerContainerManager
import com.availelabs.orchestration.containers.core.DockerImageManager
import com.availelabs.orchestration.containers.core.DockerNetworkManager
import com.availelabs.orchestration.containers.core.DockerVolumeManager
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal class ContainerOrchestrator(
    private val dockerNetworkManager: DockerNetworkManager,
    private val dockerVolumeManager: DockerVolumeManager,
    private val dockerImageManager: DockerImageManager,
    private val dockerContainerManager: DockerContainerManager
) {
    fun orchestrationSetup(runSpecifications: List<ContainerRunSpecification<*, *>>) {
        dockerNetworkManager.ensureNetworkExists()

        val namedVolumes = runSpecifications
            .flatMap { it.volumes.all }
            .filter { !it.isBindMount }
            .distinctBy(ContainerRunSpecification.VolumeMount::hostSource)

        namedVolumes.forEach { volume ->
            dockerVolumeManager.ensureVolumeExists(volume.hostSource)
        }

        runSpecifications.forEach { runSpecification ->
            dockerImageManager.ensureImageExists(runSpecification.image, runSpecification.tag).getOrThrow()
        }

        runSpecifications.forEach { runSpecification ->
            dockerContainerManager.ensureContainerIsRunning(runSpecification)
        }
    }

    fun orchestrationTeardown(runSpecifications: List<ContainerRunSpecification<*, *>>) {
        val containerNames = runSpecifications.map { it.name }
        containerNames.forEach { name ->
            dockerContainerManager.stopAndRemoveContainer(name)
        }

        val volumeNames = runSpecifications
            .flatMap { it.volumes.all }
            .filter { !it.isBindMount }
            .map(ContainerRunSpecification.VolumeMount::hostSource)
            .distinct()

        volumeNames.forEach { volumeName ->
            dockerVolumeManager.removeVolume(volumeName)
        }

        dockerNetworkManager.removeNetwork()
    }
}