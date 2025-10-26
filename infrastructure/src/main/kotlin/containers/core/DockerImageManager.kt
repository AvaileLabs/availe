package com.availelabs.containers.core

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

internal class DockerImageManager(private val dockerClient: DockerClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun ensureImageExists(imageName: String, imageTag: String): Result<Unit> = runCatching {
        val imageReference = "$imageName:$imageTag"
        val doesImageExistLocally =
            dockerClient.listImagesCmd().withImageNameFilter(imageReference).exec().isNotEmpty()
        if (!doesImageExistLocally) {
            logger.info("Image '{}' not found locally, pulling from registry...", imageReference)
            dockerClient.pullImageCmd(imageName)
                .withTag(imageTag)
                .exec(PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES)
            logger.info("Successfully pulled image '{}'.", imageReference)
        }
    }
}