package com.availelabs.containers

import com.availelabs.containers.core.DockerContainerManager
import com.availelabs.containers.core.DockerImageManager
import com.availelabs.containers.core.DockerNetworkManager
import com.availelabs.containers.core.DockerVolumeManager
import com.availelabs.containers.domain.ContainerPersistenceSpecification
import com.availelabs.containers.domain.ContainerRunSpecification


internal class ContainerOrchestrator(
    private val dockerNetworkManager: DockerNetworkManager,
    private val dockerVolumeManager: DockerVolumeManager,
    private val dockerImageManager: DockerImageManager,
    private val dockerContainerManager: DockerContainerManager
) {
    fun orchestrationSetup(runSpecifications: List<ContainerRunSpecification>) {
        dockerNetworkManager.ensureNetworkExists()

        val namedVolumeSpecifications = runSpecifications
            .flatMap { it.persistenceSpecifications }
            .filterIsInstance<ContainerPersistenceSpecification.NamedVolumePersistence>()
            .distinctBy { it.volumeName }

        for (volumeSpec in namedVolumeSpecifications) {
            dockerVolumeManager.ensureVolumeExists(volumeSpec.volumeName)
        }

        for (runSpecification in runSpecifications) {
            dockerImageManager.ensureImageExists(runSpecification.image, runSpecification.tag).getOrThrow()
        }

        for (runSpecification in runSpecifications) {
            dockerContainerManager.ensureContainerIsRunning(runSpecification)
        }
    }

    fun orchestrationTeardown(runSpecifications: List<ContainerRunSpecification>) {
        val allContainerNames = runSpecifications.map { it.name }
        for (containerName in allContainerNames) {
            dockerContainerManager.stopAndRemoveContainer(containerName)
        }

        val allVolumeNames = runSpecifications
            .flatMap { it.persistenceSpecifications }
            .filterIsInstance<ContainerPersistenceSpecification.NamedVolumePersistence>()
            .distinctBy { it.volumeName }
            .map { it.volumeName }

        for (volumeName in allVolumeNames) {
            dockerVolumeManager.removeVolume(volumeName)
        }

        dockerNetworkManager.removeNetwork()
    }
}