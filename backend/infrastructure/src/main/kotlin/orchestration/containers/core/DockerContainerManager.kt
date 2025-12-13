package com.availelabs.orchestration.containers.core

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.*
import org.slf4j.LoggerFactory

internal class DockerContainerManager(private val dockerClient: DockerClient, private val dockerNetworkName: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    sealed interface ContainerStatus {
        data class ContainerNotFound(val name: String) : ContainerStatus
        data class ContainerStopped(val containerIdentifier: String, val name: String) : ContainerStatus
        data class ContainerRunning(
            val containerIdentifier: String,
            val name: String,
            val environmentVariables: Array<String>
        ) : ContainerStatus
    }

    fun ensureContainerIsRunning(runSpecification: ContainerRunSpecification<*, *>): String {
        return when (val containerStatus = getContainerStatus(runSpecification.name)) {
            is ContainerStatus.ContainerRunning -> {
                if (isConfigurationOutOfSync(runSpecification, containerStatus)) {
                    logger.info("Configuration drift detected for container '{}'. Recreating...", runSpecification.name)
                    stopAndRemoveContainer(runSpecification.name)
                    createAndStartNewContainer(runSpecification)
                } else {
                    logger.debug("Container '{}' is already running and up to date.", runSpecification.name)
                    containerStatus.containerIdentifier
                }
            }

            is ContainerStatus.ContainerStopped -> {
                logger.info(
                    "Container '{}' is stopped. Removing and recreating to ensure fresh configuration.",
                    runSpecification.name
                )
                stopAndRemoveContainer(runSpecification.name)
                createAndStartNewContainer(runSpecification)
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

        val containerInspect = dockerClient.inspectContainerCmd(containerSummary.id).exec()
        val containerState = containerInspect.state
        val environmentVariables = containerInspect.config.env ?: emptyArray()

        return if (containerState.running == true) {
            ContainerStatus.ContainerRunning(containerSummary.id, containerName, environmentVariables)
        } else {
            ContainerStatus.ContainerStopped(containerSummary.id, containerName)
        }
    }

    private fun isConfigurationOutOfSync(
        desiredSpecification: ContainerRunSpecification<*, *>,
        actualStatus: ContainerStatus.ContainerRunning
    ): Boolean {
        val actualEnvironmentSet = actualStatus.environmentVariables.toSet()
        val desiredEnvironmentSet = desiredSpecification.environmentVariables.toSet()

        val allDesiredVariablesPresent = desiredEnvironmentSet.all { it in actualEnvironmentSet }

        if (!allDesiredVariablesPresent) {
            logger.info(
                "Environment variable mismatch for '{}'. Desired: {}, Actual: {}",
                desiredSpecification.name,
                desiredEnvironmentSet,
                actualEnvironmentSet
            )
        }

        return !allDesiredVariablesPresent
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
            Bind(volume.hostSource, Volume(volume.containerPath))
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