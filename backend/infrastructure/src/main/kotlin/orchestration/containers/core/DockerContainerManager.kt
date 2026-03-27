package com.availelabs.orchestration.containers.core

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.*
import org.apache.hc.core5.http.NoHttpResponseException
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.SocketException
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class DockerContainerManager(private val dockerClient: DockerClient, private val dockerNetworkName: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val dockerLogStreamer = DockerLogStreamer(dockerClient)

    sealed interface ContainerStatus {
        data class ContainerNotFound(val name: String) : ContainerStatus
        data class ContainerStopped(val containerIdentifier: String, val name: String) : ContainerStatus
        data class ContainerRunning(
            val containerIdentifier: String,
            val name: String,
            val environmentVariables: Array<String>,
            val imageReference: String
        ) : ContainerStatus {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ContainerRunning

                if (containerIdentifier != other.containerIdentifier) return false
                if (name != other.name) return false
                if (!environmentVariables.contentEquals(other.environmentVariables)) return false
                if (imageReference != other.imageReference) return false

                return true
            }

            override fun hashCode(): Int {
                var result = containerIdentifier.hashCode()
                result = 31 * result + name.hashCode()
                result = 31 * result + environmentVariables.contentHashCode()
                result = 31 * result + imageReference.hashCode()
                return result
            }
        }
    }

    fun ensureContainerIsRunning(runSpecification: ContainerRunSpecification<*, *>): String {
        logger.info("Ensuring container '{}' is running...", runSpecification.name)
        return when (val containerStatus = getContainerStatus(runSpecification.name)) {
            is ContainerStatus.ContainerRunning -> {
                if (isConfigurationOutOfSync(runSpecification, containerStatus)) {
                    if (runSpecification.requiresDataBackup) {
                        executeSafeUpgradeTransaction(runSpecification, containerStatus)
                    } else {
                        logger.info(
                            "Configuration drift detected for container '{}'. Recreating...",
                            runSpecification.name
                        )
                        stopAndRemoveContainer(runSpecification.name)
                        val newContainerIdentifier = createAndStartNewContainer(runSpecification)
                        runSpecification.containerHealthCheckVerificationAction?.invoke()
                        newContainerIdentifier
                    }
                } else {
                    logger.debug("Container '{}' is already running and up to date.", runSpecification.name)
                    dockerLogStreamer.startStreaming(
                        containerStatus.containerIdentifier,
                        runSpecification.name,
                        runSpecification.logStrategy
                    )
                    runSpecification.containerHealthCheckVerificationAction?.invoke()
                    containerStatus.containerIdentifier
                }
            }

            is ContainerStatus.ContainerStopped -> {
                logger.info(
                    "Container '{}' is stopped. Removing and recreating to ensure fresh configuration.",
                    runSpecification.name
                )
                stopAndRemoveContainer(runSpecification.name)
                val newContainerIdentifier = createAndStartNewContainer(runSpecification)
                runSpecification.containerHealthCheckVerificationAction?.invoke()
                newContainerIdentifier
            }

            is ContainerStatus.ContainerNotFound -> {
                logger.info("Container '{}' not found, creating and starting it.", runSpecification.name)
                val newContainerIdentifier = createAndStartNewContainer(runSpecification)
                runSpecification.containerHealthCheckVerificationAction?.invoke()
                newContainerIdentifier
            }
        }
    }

    private fun executeSafeUpgradeTransaction(
        runSpecification: ContainerRunSpecification<*, *>,
        actualStatus: ContainerStatus.ContainerRunning
    ): String {
        logger.info("Initiating safe upgrade transaction for container '{}'...", runSpecification.name)

        val originalImageReference = actualStatus.imageReference
        val hostBackupDirectoryPath = "${System.getProperty("user.dir")}/build/tmp/upgrades/${runSpecification.name}"
        val volumesToBackup = runSpecification.volumes.all.filter { !it.isBindMount }
        val dockerVolumeManager = DockerVolumeManager(dockerClient)

        dockerLogStreamer.stopStreaming(actualStatus.containerIdentifier)
        stopContainerGracefully(actualStatus.containerIdentifier, runSpecification.name)

        volumesToBackup.forEach { volumeMount ->
            val backupFileName = "${volumeMount.hostSource}-snapshot.tar"
            logger.info("Snapshotting volume '{}'...", volumeMount.hostSource)
            dockerVolumeManager.snapshotVolume(volumeMount.hostSource, hostBackupDirectoryPath, backupFileName)
        }

        removeContainer(actualStatus.containerIdentifier, runSpecification.name)
        val newContainerIdentifier = createAndStartNewContainer(runSpecification)

        val verificationAction = runSpecification.containerHealthCheckVerificationAction

        if (verificationAction != null) {
            try {
                logger.info("Verifying health for upgraded container '{}'...", runSpecification.name)
                verificationAction.invoke()
                logger.info("Health check passed. Committing upgrade.")

                val backupDirectoryFile = File(hostBackupDirectoryPath)
                if (backupDirectoryFile.exists()) {
                    backupDirectoryFile.deleteRecursively()
                }

                return newContainerIdentifier
            } catch (exception: Exception) {
                logger.error(
                    "Health check failed for upgraded container '{}': {}. Initiating rollback...",
                    runSpecification.name,
                    exception.message
                )

                stopAndRemoveContainer(runSpecification.name)

                volumesToBackup.forEach { volumeMount ->
                    logger.info("Wiping mutated volume '{}'...", volumeMount.hostSource)
                    dockerVolumeManager.removeVolume(volumeMount.hostSource)
                    dockerVolumeManager.ensureVolumeExists(volumeMount.hostSource)

                    val backupFileName = "${volumeMount.hostSource}-snapshot.tar"
                    logger.info("Restoring volume '{}' from snapshot...", volumeMount.hostSource)
                    dockerVolumeManager.restoreVolume(volumeMount.hostSource, hostBackupDirectoryPath, backupFileName)
                }

                logger.info(
                    "Redeploying container '{}' with original image '{}'...",
                    runSpecification.name,
                    originalImageReference
                )
                createAndStartNewContainer(runSpecification, originalImageReference)

                val backupDirectoryFile = File(hostBackupDirectoryPath)
                if (backupDirectoryFile.exists()) {
                    backupDirectoryFile.deleteRecursively()
                }

                throw IllegalStateException(
                    "Upgrade failed for container '${runSpecification.name}'. The system has been rolled back to the previous version and your data is safe.",
                    exception
                )
            }
        } else {
            logger.warn(
                "No verification action provided for container '{}'. Assuming successful upgrade.",
                runSpecification.name
            )

            val backupDirectoryFile = File(hostBackupDirectoryPath)
            if (backupDirectoryFile.exists()) {
                backupDirectoryFile.deleteRecursively()
            }

            return newContainerIdentifier
        }
    }

    fun getRunningContainerId(containerName: String): String {
        return when (val status = getContainerStatus(containerName)) {
            is ContainerStatus.ContainerRunning -> status.containerIdentifier
            else -> throw IllegalStateException("Container '$containerName' is not running or not found.")
        }
    }

    fun stopAndRemoveContainer(containerName: String) {
        logger.info("Attempting to stop and remove container '{}'...", containerName)
        val containerSummary = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(containerName))
            .exec()
            .firstOrNull()

        if (containerSummary == null) {
            logger.info("Container '{}' does not exist. Skipping removal.", containerName)
            return
        }

        dockerLogStreamer.stopStreaming(containerSummary.id)

        stopContainerGracefully(containerSummary.id, containerName)
        removeContainer(containerSummary.id, containerName)
    }

    private fun stopContainerGracefully(containerIdentifier: String, containerName: String) {
        try {
            val containerInspect = dockerClient.inspectContainerCmd(containerIdentifier).exec()
            val isContainerRunning = containerInspect.state.running == true

            if (isContainerRunning) {
                logger.info("Stopping container '{}' (ID: {})...", containerName, containerIdentifier)
                try {
                    dockerClient.stopContainerCmd(containerIdentifier)
                        .withTimeout(10)
                        .exec()
                    logger.info("Container '{}' stopped gracefully.", containerName)
                } catch (_: Exception) {
                    logger.warn("Failed to stop container '{}' gracefully. Attempting to kill...", containerName)
                    dockerClient.killContainerCmd(containerIdentifier).exec()
                    logger.info("Container '{}' killed.", containerName)
                }
            }
        } catch (exception: Exception) {
            logger.warn("Error during stop sequence for container '{}': {}", containerName, exception.message)
        }
    }

    private fun removeContainer(containerIdentifier: String, containerName: String) {
        try {
            logger.info("Removing container '{}'...", containerName)
            dockerClient.removeContainerCmd(containerIdentifier)
                .withForce(true)
                .exec()
            logger.info("Container '{}' removed successfully.", containerName)
        } catch (exception: Exception) {
            logger.warn("Error during remove sequence for container '{}': {}", containerName, exception.message)
        }
    }

    fun dumpLogsForContainer(containerName: String) {
        val containerSummary = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(containerName))
            .exec()
            .firstOrNull()

        if (containerSummary != null) {
            dockerLogStreamer.dumpLogs(containerSummary.id, containerName)
        } else {
            logger.warn("Could not dump logs: Container '{}' not found.", containerName)
        }
    }

    fun checkContainerForErrors(containerName: String): Boolean {
        val containerSummary = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(containerName))
            .exec()
            .firstOrNull()

        return if (containerSummary != null) {
            dockerLogStreamer.containsErrorPatterns(containerSummary.id)
        } else {
            false
        }
    }

    fun executeCommand(containerIdentifier: String, command: List<String>): String {
        try {
            performExecution(containerIdentifier, listOf("echo", "socket_check"), 1)
        } catch (_: Exception) {
            logger.debug("Socket warmup failed (expected if socket is cold), proceeding to main command.")
        }

        return performExecutionWithRetry(containerIdentifier, command)
    }

    private fun performExecutionWithRetry(containerIdentifier: String, command: List<String>): String {
        var attemptCount = 0
        val maxAttempts = 3
        var lastException: Exception? = null

        while (attemptCount < maxAttempts) {
            try {
                attemptCount++
                return performExecution(containerIdentifier, command, attemptCount)
            } catch (exception: Exception) {
                if (isTransportError(exception)) {
                    logger.warn(
                        "Transport error during exec (attempt $attemptCount/$maxAttempts). Retrying... Error: ${exception.message}"
                    )
                    lastException = exception
                    Thread.sleep(1000L * attemptCount)
                } else {
                    throw exception
                }
            }
        }
        throw IllegalStateException("Failed to execute command after $maxAttempts attempts", lastException)
    }

    private fun performExecution(containerIdentifier: String, command: List<String>, attempt: Int): String {
        logger.info("Executing command in container '{}' (Attempt {}): {}", containerIdentifier, attempt, command)

        val execCreateCmdResponse = dockerClient.execCreateCmd(containerIdentifier)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd(*command.toTypedArray())
            .exec()

        val outputStream = ByteArrayOutputStream()
        val countDownLatch = CountDownLatch(1)

        val resultCallback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame?) {
                if (frame != null && frame.payload != null) {
                    outputStream.write(frame.payload)
                }
            }

            override fun onError(throwable: Throwable?) {
                countDownLatch.countDown()
            }

            override fun onComplete() {
                countDownLatch.countDown()
            }
        }

        try {
            dockerClient.execStartCmd(execCreateCmdResponse.id)
                .exec(resultCallback)

            countDownLatch.await(30, TimeUnit.SECONDS)
        } finally {
            try {
                resultCallback.close()
            } catch (_: Exception) {
            }
        }

        val output = outputStream.toString(StandardCharsets.UTF_8)

        val inspectExecResponse = dockerClient.inspectExecCmd(execCreateCmdResponse.id).exec()
        if (inspectExecResponse.exitCodeLong != 0L) {
            throw IllegalStateException("Command failed with exit code ${inspectExecResponse.exitCodeLong}. Output: $output")
        }

        return output
    }

    private fun isTransportError(exception: Throwable): Boolean {
        var currentException: Throwable? = exception
        while (currentException != null) {
            if (currentException is NoHttpResponseException ||
                currentException is SocketException ||
                currentException is AsynchronousCloseException ||
                currentException.message?.contains("failed to respond") == true ||
                currentException.message?.contains("Connection reset") == true
            ) {
                return true
            }
            currentException = currentException.cause
        }
        return false
    }

    private fun getContainerStatus(containerName: String): ContainerStatus {
        logger.debug("Checking status for container '{}'...", containerName)
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
        val containerImageReference = containerInspect.config.image ?: ""

        return if (containerState.running == true) {
            ContainerStatus.ContainerRunning(
                containerSummary.id,
                containerName,
                environmentVariables,
                containerImageReference
            )
        } else {
            ContainerStatus.ContainerStopped(containerSummary.id, containerName)
        }
    }

    private fun isConfigurationOutOfSync(
        desiredSpecification: ContainerRunSpecification<*, *>,
        actualStatus: ContainerStatus.ContainerRunning
    ): Boolean {
        val desiredImageReference = "${desiredSpecification.image}:${desiredSpecification.tag}"
        if (actualStatus.imageReference != desiredImageReference) {
            logger.info(
                "Image tag mismatch for '{}'. Desired: {}, Actual: {}",
                desiredSpecification.name,
                desiredImageReference,
                actualStatus.imageReference
            )
            return true
        }

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

    private fun createAndStartNewContainer(
        runSpecification: ContainerRunSpecification<*, *>,
        imageReferenceOverride: String? = null
    ): String {
        logger.info("Creating new container for '{}'...", runSpecification.name)
        val imageReference = imageReferenceOverride ?: "${runSpecification.image}:${runSpecification.tag}"

        val containerPortBindings = Ports().apply {
            runSpecification.portMappings.all.forEach { portMapping ->
                bind(
                    ExposedPort.tcp(portMapping.containerPortNumber),
                    Ports.Binding.bindPort(portMapping.hostPortNumber)
                )
            }
        }

        val volumeBinds = runSpecification.volumes.all.map { volumeMount ->
            Bind(volumeMount.hostSource, Volume(volumeMount.containerPath))
        }

        val ulimits = runSpecification.ulimits.map {
            Ulimit(it.name, it.softLimit, it.hardLimit)
        }

        val hostConfiguration = HostConfig.newHostConfig()
            .withPortBindings(containerPortBindings)
            .withBinds(volumeBinds)
            .withUlimits(ulimits)
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
        logger.info("Container created. Identifier: {}. Starting container...", containerIdentifier)

        dockerLogStreamer.startStreaming(
            containerIdentifier,
            runSpecification.name,
            runSpecification.logStrategy
        )

        dockerClient.startContainerCmd(containerIdentifier).exec()
        logger.info("Container '{}' started successfully.", runSpecification.name)

        return containerIdentifier
    }
}