package com.availelabs.orchestration.containers

import com.availelabs.orchestration.containers.core.DockerContainerManager
import com.availelabs.orchestration.containers.core.DockerImageManager
import com.availelabs.orchestration.containers.core.DockerNetworkManager
import com.availelabs.orchestration.containers.core.DockerVolumeManager
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.containers.domain.ContainerTransitionState
import com.availelabs.orchestration.containers.domain.DeploymentGroup
import org.slf4j.LoggerFactory

internal class ContainerOrchestrator(
    private val dockerNetworkManager: DockerNetworkManager,
    private val dockerVolumeManager: DockerVolumeManager,
    private val dockerImageManager: DockerImageManager,
    private val dockerContainerManager: DockerContainerManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun orchestrationSetup(deploymentGroups: List<DeploymentGroup>) {
        dockerNetworkManager.ensureNetworkExists()

        val allContainerRunSpecifications = deploymentGroups.flatMap { it.containerRunSpecifications }

        val namedVolumes = allContainerRunSpecifications
            .flatMap { it.volumes.all }
            .filter { !it.isBindMount }
            .distinctBy(ContainerRunSpecification.VolumeMount::hostSource)

        namedVolumes.forEach { volumeMount ->
            dockerVolumeManager.ensureVolumeExists(volumeMount.hostSource)
        }

        allContainerRunSpecifications.forEach { containerRunSpecification ->
            dockerImageManager.ensureImageExists(containerRunSpecification.image, containerRunSpecification.tag)
                .getOrThrow()
        }

        deploymentGroups.forEach { deploymentGroup ->
            val containerTransitionStates =
                deploymentGroup.containerRunSpecifications.map { containerRunSpecification ->
                    dockerContainerManager.ensureContainerIsRunning(containerRunSpecification)
                }

            val groupHealthCheckVerificationAction = deploymentGroup.groupHealthCheckVerificationAction

            var isDeploymentSuccessful = true
            var capturedException: Exception? = null

            if (groupHealthCheckVerificationAction != null) {
                try {
                    logger.info("Verifying health for deployment group '{}'...", deploymentGroup.deploymentGroupName)
                    groupHealthCheckVerificationAction.invoke()
                } catch (exception: Exception) {
                    isDeploymentSuccessful = false
                    capturedException = exception
                }
            }

            if (isDeploymentSuccessful) {
                containerTransitionStates.forEach { containerTransitionState ->
                    when (containerTransitionState) {
                        is ContainerTransitionState.AlreadyRunningAndUpToDate -> {
                        }

                        is ContainerTransitionState.FreshDeployment -> {
                            containerTransitionState.pendingCommitAction.invoke()
                        }

                        is ContainerTransitionState.UpgradePending -> {
                            logger.info(
                                "Committing upgrade for container '{}'.",
                                containerTransitionState.containerName
                            )
                            containerTransitionState.pendingCommitAction.invoke()
                        }
                    }
                }
            } else {
                containerTransitionStates.reversed().forEach { containerTransitionState ->
                    when (containerTransitionState) {
                        is ContainerTransitionState.AlreadyRunningAndUpToDate -> {
                        }

                        is ContainerTransitionState.FreshDeployment -> {
                            logger.error(
                                "Health check failed for deployment group '{}'. Rolling back fresh container '{}'...",
                                deploymentGroup.deploymentGroupName,
                                containerTransitionState.containerName
                            )
                            containerTransitionState.pendingRollbackAction.invoke()
                        }

                        is ContainerTransitionState.UpgradePending -> {
                            logger.error(
                                "Health check failed for deployment group '{}'. Rolling back upgraded container '{}'...",
                                deploymentGroup.deploymentGroupName,
                                containerTransitionState.containerName
                            )
                            containerTransitionState.pendingRollbackAction.invoke()
                        }
                    }
                }

                if (capturedException != null) {
                    throw IllegalStateException(
                        "Health check failed for deployment group '${deploymentGroup.deploymentGroupName}'. The system has been rolled back and your data is safe.",
                        capturedException
                    )
                }
            }
        }
    }

    fun orchestrationTeardown(deploymentGroups: List<DeploymentGroup>) {
        val allContainerRunSpecifications = deploymentGroups.flatMap { it.containerRunSpecifications }

        val containerNames = allContainerRunSpecifications.map { it.name }
        containerNames.forEach { containerName ->
            dockerContainerManager.stopAndRemoveContainer(containerName)
        }

        val volumeNames = allContainerRunSpecifications
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