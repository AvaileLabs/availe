package com.availelabs.orchestration.containers.core

import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory

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
                logger.info("Volume '{}' not found, creating it.", volumeName)
                dockerClient.createVolumeCmd()
                    .withName(volumeName)
                    .exec()
            } else {
                logger.info("Volume '{}' already exists, continuing.", volumeName)
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

            if (dockerVolume == null) {
                logger.info("Volume '{}' was not found, ignoring removal.", volumeName)
                return@executeWithRetry
            }

            logger.info("Removing volume '{}'...", volumeName)
            dockerClient.removeVolumeCmd(dockerVolume.name).exec()
        }
    }
}