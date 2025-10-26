package com.availelabs.containers.core

import com.availelabs.containers.domain.ContainerRunSpecification
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.*
import org.slf4j.LoggerFactory

internal class DockerContainerManager(private val dockerClient: DockerClient, private val dockerNetworkName: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    sealed interface ContainerStatus {
        data class ContainerNotFound(val name: String) : ContainerStatus
        data class ContainerStopped(val containerIdentifier: String, val name: String) : ContainerStatus
        data class ContainerRunning(val containerIdentifier: String, val name: String) : ContainerStatus
    }

    fun ensureContainerIsRunning(runSpecification: ContainerRunSpecification<*, *>): String {
        return when (val containerStatus = getContainerStatus(runSpecification.name)) {
            is ContainerStatus.ContainerRunning -> {
                logger.debug("Container '{}' is already running.", runSpecification.name)
                containerStatus.containerIdentifier
            }

            is ContainerStatus.ContainerStopped -> {
                logger.info("Container '{}' is stopped, starting it now.", runSpecification.name)
                dockerClient.startContainerCmd(containerStatus.containerIdentifier).exec()
                containerStatus.containerIdentifier
            }

            is ContainerStatus.ContainerNotFound -> {
                logger.info("Container '{}' not found, creating and starting it.", runSpecification.name)
                createAndStartNewContainer(runSpecification)
            }
        }
    }

    fun stopAndRemoveContainer(containerName: String) {
        val containerSummary = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(containerName))
            .exec()
            .firstOrNull() ?: return

        try {
            val isContainerRunning = dockerClient.inspectContainerCmd(containerSummary.id).exec().state.running == true
            if (isContainerRunning) {
                logger.info("Stopping container '{}'...", containerName)
                dockerClient.stopContainerCmd(containerSummary.id).exec()
            }
            logger.info("Removing container '{}'...", containerName)
            dockerClient.removeContainerCmd(containerSummary.id).exec()
        } catch (_: Exception) {
            logger.warn("Container '{}' was already removed or failed to stop/remove, ignoring.", containerName)
        }
    }

    private fun getContainerStatus(containerName: String): ContainerStatus {
        val containerSummary = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(containerName))
            .exec()
            .firstOrNull()

        if (containerSummary == null) {
            return ContainerStatus.ContainerNotFound(containerName)
        }

        val containerState = dockerClient.inspectContainerCmd(containerSummary.id).exec().state
        return if (containerState.running == true) {
            ContainerStatus.ContainerRunning(containerSummary.id, containerName)
        } else {
            ContainerStatus.ContainerStopped(containerSummary.id, containerName)
        }
    }

    private fun createAndStartNewContainer(runSpecification: ContainerRunSpecification<*, *>): String {
        val imageReference = "${runSpecification.image}:${runSpecification.tag}"

        val containerPortBindings = Ports().apply {
            runSpecification.portMappings.all.forEach { portMapping ->
                bind(
                    ExposedPort.tcp(portMapping.containerPortNumber),
                    Ports.Binding.bindPort(portMapping.hostPortNumber)
                )
            }
        }

        val volumeBinds = runSpecification.volumes.all.map { volume ->
            Bind(volume.volumeName, Volume(volume.containerPath))
        }

        val hostConfiguration = HostConfig.newHostConfig()
            .withPortBindings(containerPortBindings)
            .withBinds(volumeBinds)
            .withRestartPolicy(runSpecification.restartPolicy)
            .withNetworkMode(dockerNetworkName)

        val createContainerCommand = dockerClient.createContainerCmd(imageReference)
            .withName(runSpecification.name)
            .withEnv(runSpecification.environmentVariables)
            .withExposedPorts(containerPortBindings.bindings.keys.toList())
            .withHostConfig(hostConfiguration)

        if (runSpecification.command.isNotEmpty()) {
            createContainerCommand.withCmd(runSpecification.command)
        }

        val containerIdentifier = createContainerCommand.exec().id
        dockerClient.startContainerCmd(containerIdentifier).exec()
        return containerIdentifier
    }
}