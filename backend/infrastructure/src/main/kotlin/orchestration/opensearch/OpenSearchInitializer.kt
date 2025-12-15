package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.orchestration.containers.core.DockerContainerManager
import org.slf4j.LoggerFactory
import java.io.File

internal class OpenSearchInitializer(private val dockerContainerManager: DockerContainerManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun initializeSecurity() {
        val containerName = SharedOpenSearchConstants.NODE_1_NAME
        val port = SharedOpenSearchConstants.OPENSEARCH_PORT

        val projectRootDirectory = System.getProperty("user.dir")
        val securityConfigurationDirectory =
            File(projectRootDirectory, "build/generated/opensearch/opensearch-security")

        val currentConfigurationHash =
            OpenSearchSecurityHasher.calculateConfigurationHash(securityConfigurationDirectory)
        val openSearchSecurityReconciler = OpenSearchSecurityReconciler()

        logger.info("Waiting for OpenSearch API to be available...")
        openSearchSecurityReconciler.waitForClusterAvailability(60)

        if (!openSearchSecurityReconciler.isReconciliationNeeded(currentConfigurationHash)) {
            logger.info("OpenSearch Security is already up to date. Skipping initialization.")
            return
        }

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

            openSearchSecurityReconciler.updateStateWithNewHash(currentConfigurationHash)

        } catch (exception: Exception) {
            logger.error("Failed to initialize OpenSearch Security: ${exception.message}", exception)
            throw IllegalStateException("OpenSearch Security Initialization failed.", exception)
        }
    }
}