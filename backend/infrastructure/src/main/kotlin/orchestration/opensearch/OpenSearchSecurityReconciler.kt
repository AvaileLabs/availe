package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal class OpenSearchSecurityReconciler {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    private val stateIndexName = "availe-infra-state"
    private val stateDocumentId = "security-config-hash"

    fun waitForClusterAvailability(timeoutSeconds: Long) {
        val httpClient = createSecureHttpClient()
        val port = SharedOpenSearchConstants.OPENSEARCH_PORT
        val uri = URI.create("https://localhost:$port/_cluster/health")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val deadline = System.currentTimeMillis() + (timeoutSeconds * 1000)

        logger.info("Polling OpenSearch cluster health at $uri...")

        while (System.currentTimeMillis() < deadline) {
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    val jsonNode = objectMapper.readTree(response.body())
                    val status = jsonNode.path("status").asText()

                    if (status == "green" || status == "yellow") {
                        logger.info("OpenSearch cluster is ready (Status: $status).")
                        return
                    } else {
                        logger.debug("OpenSearch cluster status is '$status'. Waiting for yellow/green...")
                    }
                } else {
                    logger.debug("OpenSearch health check returned status ${response.statusCode()}.")
                }
            } catch (exception: Exception) {
                logger.trace("OpenSearch connection failed: ${exception.message}. Retrying...")
            }

            try {
                Thread.sleep(1000)
            } catch (interruptedException: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Interrupted while waiting for OpenSearch", interruptedException)
            }
        }
        throw IllegalStateException("OpenSearch API failed to become ready (Yellow/Green) within $timeoutSeconds seconds.")
    }

    fun isReconciliationNeeded(generatedHash: String): Boolean {
        val httpClient = createSecureHttpClient()
        val port = SharedOpenSearchConstants.OPENSEARCH_PORT

        ensureStateIndexExists(httpClient, port)

        val uri = URI.create("https://localhost:$port/$stateIndexName/_doc/$stateDocumentId")
        val request = HttpRequest.newBuilder(uri).GET().build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val jsonNode = objectMapper.readTree(response.body())
                val remoteHash = jsonNode.path("_source").path("hash").asText()

                if (remoteHash == generatedHash) {
                    logger.info("OpenSearch Security Configuration is up to date.")
                    false
                } else {
                    logger.info("Configuration hash mismatch. Local: $generatedHash, Remote: $remoteHash")
                    true
                }
            } else if (response.statusCode() == 404) {
                logger.info("No existing security state found (404). Initialization required.")
                true
            } else {
                logger.warn("Unexpected status code from state index: ${response.statusCode()}. Defaulting to reconciliation.")
                true
            }
        } catch (exception: Exception) {
            logger.warn("Failed to check OpenSearch state. Defaulting to reconciliation needed. Error: ${exception.message}")
            true
        }
    }

    fun updateStateWithNewHash(generatedHash: String) {
        val httpClient = createSecureHttpClient()
        val port = SharedOpenSearchConstants.OPENSEARCH_PORT
        val uri = URI.create("https://localhost:$port/$stateIndexName/_doc/$stateDocumentId")

        val jsonBody = """
            {
                "hash": "$generatedHash",
                "updated_at": "${java.time.Instant.now()}"
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                logger.info("Successfully updated OpenSearch state with new configuration hash.")
            } else {
                logger.warn("Failed to update state hash. Status: ${response.statusCode()} Body: ${response.body()}")
            }
        } catch (exception: Exception) {
            logger.error("Failed to update OpenSearch state hash: ${exception.message}", exception)
        }
    }

    private fun ensureStateIndexExists(httpClient: HttpClient, port: Int) {
        val uri = URI.create("https://localhost:$port/$stateIndexName")
        val request = HttpRequest.newBuilder(uri)
            .PUT(HttpRequest.BodyPublishers.ofString("{}"))
            .build()

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        } catch (_: Exception) {
        }
    }

    private fun createSecureHttpClient(): HttpClient {
        val projectRootDirectory = System.getProperty("user.dir")
        val generatedCertificatesDirectory = File(projectRootDirectory, "build/generated/opensearch/certs")

        val rootCaFile = File(generatedCertificatesDirectory, "root-ca.pem")
        val adminCertificateFile = File(generatedCertificatesDirectory, "admin.pem")
        val adminKeyFile = File(generatedCertificatesDirectory, "admin-key.pem")

        if (!rootCaFile.exists() || !adminCertificateFile.exists() || !adminKeyFile.exists()) {
            throw IllegalStateException("Cannot create secure HTTP client. Certificates are missing in ${generatedCertificatesDirectory.absolutePath}")
        }

        val sslContext = OpenSearchSslContextFactory.createAdminSslContext(
            rootCaFile,
            adminCertificateFile,
            adminKeyFile
        )

        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(2))
            .build()
    }
}