package com.availelabs.orchestration.containers.core

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

internal class DockerVolumeManager(private val dockerClient: DockerClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun ensureVolumeExists(volumeName: String) {
        DockerOperationRetrier.executeWithRetry("Ensure Docker Volume Exists") {
            val doesVolumeExist = dockerClient.listVolumesCmd()
                .withFilter("name", listOf(volumeName))
                .exec()
                .volumes
                .isNotEmpty()

            if (!doesVolumeExist) {
                dockerClient.createVolumeCmd()
                    .withName(volumeName)
                    .exec()
            }
        }
    }

    fun removeVolume(volumeName: String) {
        DockerOperationRetrier.executeWithRetry("Remove Docker Volume") {
            val dockerVolume = dockerClient.listVolumesCmd()
                .withFilter("name", listOf(volumeName))
                .exec()
                .volumes
                .firstOrNull()

            if (dockerVolume != null) {
                dockerClient.removeVolumeCmd(dockerVolume.name).exec()
            }
        }
    }

    fun snapshotVolume(volumeName: String, hostBackupDirectoryPath: String, backupFileName: String) {
        ensureAlpineImageExists()

        val hostBackupDirectoryFile = File(hostBackupDirectoryPath)
        if (!hostBackupDirectoryFile.exists()) {
            hostBackupDirectoryFile.mkdirs()
        }

        val containerVolumeMount = Volume("/data_volume")
        val containerBackupMount = Volume("/backup_directory")

        val volumeBind = Bind(volumeName, containerVolumeMount)
        val backupDirectoryBind = Bind(hostBackupDirectoryPath, containerBackupMount)

        val hostConfiguration = HostConfig.newHostConfig().withBinds(volumeBind, backupDirectoryBind)

        val createContainerResponse = dockerClient.createContainerCmd("docker.io/library/alpine:latest")
            .withHostConfig(hostConfiguration)
            .withCmd("tar", "-cvf", "/backup_directory/$backupFileName", "-C", "/data_volume", ".")
            .exec()

        val temporaryContainerIdentifier = createContainerResponse.id

        try {
            dockerClient.startContainerCmd(temporaryContainerIdentifier).exec()
            val waitContainerResultCallback = WaitContainerResultCallback()
            dockerClient.waitContainerCmd(temporaryContainerIdentifier).exec(waitContainerResultCallback)
            val exitCode = waitContainerResultCallback.awaitStatusCode(5, TimeUnit.MINUTES)

            if (exitCode != 0) {
                throw IllegalStateException("Snapshot operation failed with exit code $exitCode for volume $volumeName")
            }
        } finally {
            dockerClient.removeContainerCmd(temporaryContainerIdentifier).withForce(true).exec()
        }
    }

    fun restoreVolume(volumeName: String, hostBackupDirectoryPath: String, backupFileName: String) {
        ensureAlpineImageExists()

        val containerVolumeMount = Volume("/data_volume")
        val containerBackupMount = Volume("/backup_directory")

        val volumeBind = Bind(volumeName, containerVolumeMount)
        val backupDirectoryBind = Bind(hostBackupDirectoryPath, containerBackupMount)

        val hostConfiguration = HostConfig.newHostConfig().withBinds(volumeBind, backupDirectoryBind)

        val createContainerResponse = dockerClient.createContainerCmd("docker.io/library/alpine:latest")
            .withHostConfig(hostConfiguration)
            .withCmd("sh", "-c", "rm -rf /data_volume/* && tar -xvf /backup_directory/$backupFileName -C /data_volume")
            .exec()

        val temporaryContainerIdentifier = createContainerResponse.id

        try {
            dockerClient.startContainerCmd(temporaryContainerIdentifier).exec()
            val waitContainerResultCallback = WaitContainerResultCallback()
            dockerClient.waitContainerCmd(temporaryContainerIdentifier).exec(waitContainerResultCallback)
            val exitCode = waitContainerResultCallback.awaitStatusCode(5, TimeUnit.MINUTES)

            if (exitCode != 0) {
                throw IllegalStateException("Restore operation failed with exit code $exitCode for volume $volumeName")
            }
        } finally {
            dockerClient.removeContainerCmd(temporaryContainerIdentifier).withForce(true).exec()
        }
    }

    private fun ensureAlpineImageExists() {
        DockerOperationRetrier.executeWithRetry("Ensure Alpine Image Exists") {
            val doesImageExistLocally = dockerClient.listImagesCmd()
                .withReferenceFilter("docker.io/library/alpine:latest")
                .exec()
                .isNotEmpty()

            if (!doesImageExistLocally) {
                dockerClient.pullImageCmd("docker.io/library/alpine")
                    .withTag("latest")
                    .exec(PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES)
            }
        }
    }
}