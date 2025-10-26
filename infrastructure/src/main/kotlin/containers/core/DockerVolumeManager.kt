package com.availelabs.containers.core

import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory

internal class DockerVolumeManager(private val dockerClient: DockerClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun ensureVolumeExists(volumeName: String) {
        val doesVolumeExist =
            dockerClient.listVolumesCmd().withFilter("name", listOf(volumeName)).exec().volumes.isNotEmpty()
        if (!doesVolumeExist) {
            logger.info("Volume '{}' not found, creating it.", volumeName)
            dockerClient.createVolumeCmd().withName(volumeName).exec()
        }
        logger.info("Volume '{}' already exists, continuing.", volumeName)
    }

    fun removeVolume(volumeName: String) {
        val dockerVolume =
            dockerClient.listVolumesCmd().withFilter("name", listOf(volumeName)).exec().volumes.firstOrNull()

        if (dockerVolume == null) {
            logger.info("Volume '{}' was not found, ignoring removal.", volumeName)
            return
        }

        logger.info("Removing volume '{}'...", volumeName)
        dockerClient.removeVolumeCmd(dockerVolume.name).exec()
    }
}