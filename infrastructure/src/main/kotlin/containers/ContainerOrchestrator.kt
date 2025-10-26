package com.availelabs.containers

import com.availelabs.containers.core.DockerContainerManager
import com.availelabs.containers.core.DockerImageManager
import com.availelabs.containers.core.DockerNetworkManager
import com.availelabs.containers.core.DockerVolumeManager
import com.availelabs.containers.domain.ContainerRunSpecification

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
            .distinctBy(ContainerRunSpecification.VolumeMount::volumeName)

        namedVolumes.forEach { volume ->
            dockerVolumeManager.ensureVolumeExists(volume.volumeName)
        }

        runSpecifications.forEach { spec ->
            dockerImageManager.ensureImageExists(spec.image, spec.tag).getOrThrow()
        }

        runSpecifications.forEach { spec ->
            dockerContainerManager.ensureContainerIsRunning(spec)
        }
    }

    fun orchestrationTeardown(runSpecifications: List<ContainerRunSpecification<*, *>>) {
        val containerNames = runSpecifications.map { it.name }
        containerNames.forEach { name ->
            dockerContainerManager.stopAndRemoveContainer(name)
        }

        val volumeNames = runSpecifications
            .flatMap { it.volumes.all }
            .map(ContainerRunSpecification.VolumeMount::volumeName)
            .distinct()

        volumeNames.forEach { volumeName ->
            dockerVolumeManager.removeVolume(volumeName)
        }

        dockerNetworkManager.removeNetwork()
    }
}