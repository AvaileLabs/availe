package com.availelabs.utils

import com.availelabs.keycloak.KeycloakPortsList
import com.availelabs.postgres.PostgresConfig
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.time.Duration
import java.util.*

internal object ServiceWaiter {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun waitForPostgres(config: PostgresConfig, timeoutSeconds: Long = 60) {
        logger.info("Waiting for database connection at ${config.dbUrl}...")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        val props = Properties().apply {
            setProperty("user", config.dbUser)
            setProperty("password", config.dbPassword)
        }
        var lastError: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                DriverManager.getConnection(config.dbUrl, props).use { conn ->
                    conn.createStatement().use { st ->
                        st.execute("SELECT 1")
                    }
                }
                logger.info("Database connection successful.")
                return
            } catch (t: Throwable) {
                lastError = t
                Thread.sleep(1_000)
            }
        }
        throw IllegalStateException("Database not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }

    fun waitForKeycloak(timeoutSeconds: Long = 120) {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build()

        val port = KeycloakPortsList.Management.hostPortNumber
        val healthUri = URI.create("http://localhost:$port/health/ready")
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Throwable? = null

        logger.info("Waiting for Keycloak to be ready at {}...", healthUri)
        while (System.currentTimeMillis() < deadline) {
            try {
                val request = HttpRequest.newBuilder(healthUri)
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build()
                val resp = client.send(request, HttpResponse.BodyHandlers.discarding())
                if (resp.statusCode() == 200) {
                    logger.info("Keycloak is ready.")
                    return
                }
            } catch (t: Throwable) {
                lastError = t
            }
            Thread.sleep(1_000)
        }
        throw IllegalStateException("Keycloak not ready after ${timeoutSeconds}s: ${lastError?.message}", lastError)
    }
}