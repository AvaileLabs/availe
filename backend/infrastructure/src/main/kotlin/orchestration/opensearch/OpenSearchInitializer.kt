package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.orchestration.containers.core.DockerContainerManager
import org.slf4j.LoggerFactory

internal class OpenSearchInitializer(private val dockerContainerManager: DockerContainerManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun initializeSecurity() {
        logger.info("Forcing OpenSearch Security Configuration via securityadmin.sh...")

        val containerName = SharedOpenSearchConstants.NODE_1_NAME

        // We explicitly specify -h (hostname) and -p (port)
        // This ensures the script connects to the node using the interface and port
        // that matches the SSL certificate we generated (CN=opensearch-node1).
        // Fix: Targeted the HTTP/REST port (9200) instead of Transport port (9300) for OpenSearch 3.x+

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
                    "-p ${SharedOpenSearchConstants.OPENSEARCH_PORT}"
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
}