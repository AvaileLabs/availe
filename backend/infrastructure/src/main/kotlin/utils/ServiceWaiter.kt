package com.availelabs.utils

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.ejabberd.EjabberdPortsList
import com.availelabs.orchestration.keycloak.KeycloakPortsList
import com.availelabs.orchestration.postgres.PostgresConfig
import org.slf4j.LoggerFactory
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.time.Duration
import java.util.*

internal object ServiceWaiter {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun waitForPostgres(postgresConfiguration: PostgresConfig, timeoutSeconds: Long = 60) {
        logger.info("Waiting for database connection at ${postgresConfiguration.dbUrl}...")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        val properties = Properties().apply {
            setProperty("user", postgresConfiguration.dbUser)
            setProperty("password", postgresConfiguration.dbPassword)
        }
        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                DriverManager.getConnection(postgresConfiguration.dbUrl, properties).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("SELECT 1")
                    }
                }
                logger.info("Database connection successful.")
                return
            } catch (throwable: Throwable) {
                lastError = throwable
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

    fun waitForEjabberd(timeoutSeconds: Long = 60) {
        val port = EjabberdPortsList.Client.hostPortNumber
        val host = "localhost"
        logger.info("Waiting for Ejabberd to be ready at $host:$port...")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                Socket(host, port).use { socket ->
                    if (socket.isConnected) {
                        logger.info("Ejabberd is ready.")
                        return
                    }
                }
            } catch (throwable: Throwable) {
                lastError = throwable
            }
            Thread.sleep(1_000)
        }
        throw IllegalStateException("Ejabberd not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }
}