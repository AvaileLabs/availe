package com.availelabs.utils

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.core.DockerContainerManager
import com.availelabs.orchestration.keycloak.KeycloakPortsList
import com.availelabs.orchestration.opensearch.OpenSearchConfig
import com.availelabs.orchestration.opensearch.OpenSearchPortsList
import com.availelabs.orchestration.postgres.PostgresConfig
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.sql.DriverManager
import java.time.Duration
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

internal class ServiceWaiter(private val dockerContainerManager: DockerContainerManager) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun waitForPostgres(postgresConfiguration: PostgresConfig, timeoutSeconds: Long = 60) {
        logger.info("Waiting for database connection at ${postgresConfiguration.dbUrl}...")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000

        val connectionProperties = Properties().apply {
            setProperty("user", postgresConfiguration.dbUser)
            setProperty("password", postgresConfiguration.dbPassword)
            setProperty("loginTimeout", "5")
            setProperty("socketTimeout", "5")
            setProperty("connectTimeout", "5")
        }

        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                DriverManager.getConnection(postgresConfiguration.dbUrl, connectionProperties).use { connection ->
                    if (connection.isValid(5)) {
                        logger.info("Database connection successful.")
                        return
                    }
                }
            } catch (throwable: Throwable) {
                lastError = throwable
                logger.debug("Database not yet ready: ${throwable.message}")
                Thread.sleep(1_000)
            }
        }
        throw IllegalStateException("Database not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }

    fun waitForKeycloak(timeoutSeconds: Long = 120) {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build()

        val port = KeycloakPortsList.Management.hostPortNumber
        val healthUri = URI.create("http://localhost:$port${SharedNetworkConstants.KEYCLOAK_HEALTH_ENDPOINT_PATH}")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Throwable? = null

        logger.info("Waiting for Keycloak to be ready at {}...", healthUri)
        while (System.currentTimeMillis() < deadline) {
            try {
                val httpRequest = HttpRequest.newBuilder(healthUri)
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build()
                val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding())
                if (httpResponse.statusCode() == 200) {
                    logger.info("Keycloak is ready.")
                    return
                }
            } catch (throwable: Throwable) {
                lastError = throwable
            }
            Thread.sleep(1_000)
        }
        throw IllegalStateException("Keycloak not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }

    fun waitForEjabberd(containerName: String, timeoutSeconds: Long = 60) {
        logger.info("Waiting for Ejabberd to be ready (via ejabberdctl status)...")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Throwable? = null

        val statusCommand = listOf("ejabberdctl", "status")

        while (System.currentTimeMillis() < deadline) {
            try {
                val commandOutput = dockerContainerManager.executeCommand(containerName, statusCommand)
                if (commandOutput.contains("The node", ignoreCase = true) &&
                    commandOutput.contains("is started", ignoreCase = true)
                ) {
                    logger.info("Ejabberd is ready.")
                    return
                }
            } catch (throwable: Throwable) {
                lastError = throwable
                logger.debug("Ejabberd not yet ready: ${throwable.message}")
            }
            Thread.sleep(2_000)
        }
        throw IllegalStateException("Ejabberd not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }

    fun waitForOpenSearch(openSearchConfig: OpenSearchConfig, timeoutSeconds: Long = 60) {
        val port = OpenSearchPortsList.RestApi.hostPortNumber
        val host = "localhost"
        val healthUri = URI.create("https://$host:$port/_cluster/health")
        logger.info("Waiting for OpenSearch to be ready at $healthUri...")

        val projectRootDirectory = System.getProperty("user.dir")
        val rootCaFile = File(projectRootDirectory, "build/generated/opensearch/certs/root-ca.pem")
        if (!rootCaFile.exists()) {
            throw IllegalStateException("OpenSearch Root CA not found at ${rootCaFile.absolutePath}")
        }

        val sslContext = createSslContextWithRootCa(rootCaFile)
        val httpClient = HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(1))
            .build()

        val basicAuth = Base64.getEncoder()
            .encodeToString("admin:${openSearchConfig.adminPassword}".toByteArray())

        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        val startTime = System.currentTimeMillis()
        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                val httpRequest = HttpRequest.newBuilder(healthUri)
                    .header("Authorization", "Basic $basicAuth")
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build()

                val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

                if (httpResponse.statusCode() == 200) {
                    logger.info("OpenSearch is ready (Status 200). Cluster health: ${httpResponse.body()}")
                    return
                } else if (httpResponse.statusCode() == 401 || httpResponse.statusCode() == 403) {
                    if (System.currentTimeMillis() - startTime < 30_000) {
                        logger.debug("OpenSearch Auth Failed (Init Phase). Status: ${httpResponse.statusCode()}")
                    } else {
                        logger.warn("OpenSearch Authentication Failed (Status ${httpResponse.statusCode()}). Body: ${httpResponse.body()}")
                    }
                } else {
                    logger.debug("OpenSearch returned status code ${httpResponse.statusCode()}")
                }
            } catch (throwable: Throwable) {
                lastError = throwable
            }
            Thread.sleep(1_000)
        }
        throw IllegalStateException("OpenSearch not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }

    private fun createSslContextWithRootCa(pemFile: File): SSLContext {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate = ByteArrayInputStream(pemFile.readBytes()).use {
            certificateFactory.generateCertificate(it) as X509Certificate
        }

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("opensearch-root-ca", certificate)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        return sslContext
    }
}