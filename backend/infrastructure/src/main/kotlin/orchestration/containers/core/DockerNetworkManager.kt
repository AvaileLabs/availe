package com.availelabs.orchestration.containers.core

import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory

internal class DockerNetworkManager(private val dockerClient: DockerClient, private val networkName: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun ensureNetworkExists() {
        DockerOperationRetrier.executeWithRetry("Ensure Docker Network Exists") {
            val doesNetworkExist = dockerClient.listNetworksCmd()
                .withNameFilter(networkName)
                .exec()
                .isNotEmpty()

            if (!doesNetworkExist) {
                logger.info("Network '{}' not found, creating it.", networkName)
                dockerClient.createNetworkCmd()
                    .withName(networkName)
                    .exec()
            } else {
                logger.info("Network '{}' already exists, continuing.", networkName)
            }
        }
    }

    fun removeNetwork() {
        DockerOperationRetrier.executeWithRetry("Remove Docker Network") {
            val dockerNetwork = dockerClient.listNetworksCmd()
                .withNameFilter(networkName)
                .exec()
                .firstOrNull()

            if (dockerNetwork == null) {
                logger.info("Network '{}' was not found, ignoring removal.", networkName)
                return@executeWithRetry
            }

            logger.info("Removing network '{}'.", networkName)
            dockerClient.removeNetworkCmd(dockerNetwork.id).exec()
        }
    }
}