package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.orchestration.containers.core.DockerContainerManager
import org.slf4j.LoggerFactory

internal class OpenSearchInitializer(private val dockerContainerManager: DockerContainerManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun initializeSecurity() {
        val containerName = SharedOpenSearchConstants.NODE_1_NAME
        val port = SharedOpenSearchConstants.OPENSEARCH_PORT

        logger.info("Waiting for OpenSearch internal port $port to be available inside container '$containerName'...")
        waitForInternalPortAvailability(containerName, port)

        logger.info("Forcing OpenSearch Security Configuration via securityadmin.sh...")

        val command = listOf(
            "/bin/bash",
            "-c",
            "chmod +x /usr/share/opensearch/plugins/opensearch-security/tools/securityadmin.sh && " +
                    "/usr/share/opensearch/plugins/opensearch-security/tools/securityadmin.sh " +
                    "-cd /usr/share/opensearch/config/opensearch-security/ " +
                    "-icl " +
                    "-nhnv " +
                    "-cacert /usr/share/opensearch/config/certs/root-ca.pem " +
                    "-cert /usr/share/opensearch/config/certs/admin.pem " +
                    "-key /usr/share/opensearch/config/certs/admin-key.pem " +
                    "-h $containerName " +
                    "-p $port"
        )

        try {
            val output = dockerContainerManager.executeCommand(containerName, command)
            logger.info("Security Initialization Output:\n$output")
            logger.info("OpenSearch Security Configuration successfully applied.")

        } catch (exception: Exception) {
            logger.error("Failed to initialize OpenSearch Security: ${exception.message}", exception)
            throw IllegalStateException("OpenSearch Security Initialization failed.", exception)
        }
    }

    private fun waitForInternalPortAvailability(containerName: String, port: Int) {
        // Loops for up to 60 seconds inside the container checking for TCP connectivity on the localhost port.
        // Returns exit code 0 if successful, 1 if timeout.
        val waitCommand = listOf(
            "/bin/bash",
            "-c",
            "for i in {1..60}; do if timeout 1 bash -c 'cat < /dev/null > /dev/tcp/localhost/$port' 2>/dev/null; then exit 0; fi; sleep 1; done; echo 'Timeout waiting for port $port'; exit 1"
        )

        try {
            dockerContainerManager.executeCommand(containerName, waitCommand)
            logger.info("Internal port $port is accepting connections.")
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Timed out waiting for OpenSearch port $port to open inside container '$containerName'.",
                exception
            )
        }
    }
}