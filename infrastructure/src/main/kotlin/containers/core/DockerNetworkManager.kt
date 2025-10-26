package com.availelabs.containers.core

import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory

internal class DockerNetworkManager(private val dockerClient: DockerClient, private val networkName: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun ensureNetworkExists() {
        val doesNetworkExist = dockerClient.listNetworksCmd().withNameFilter(networkName).exec().isNotEmpty()
        if (!doesNetworkExist) {
            logger.info("Network '{}' not found, creating it.", networkName)
            dockerClient.createNetworkCmd().withName(networkName).exec()
        }
        logger.info("Network '{}' already exists, continuing.", networkName)
    }

    fun removeNetwork() {
        val dockerNetwork = dockerClient.listNetworksCmd().withNameFilter(networkName).exec().firstOrNull()
        if (dockerNetwork == null) {
            logger.info("Network '{}' was not found, ignoring removal.", networkName)
            return
        }
        logger.info("Removing network '{}'.", networkName)
        dockerClient.removeNetworkCmd(dockerNetwork.id).exec()
    }
}