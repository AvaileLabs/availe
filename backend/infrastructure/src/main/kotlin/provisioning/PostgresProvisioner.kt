package com.availelabs.provisioning

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.keycloak.specs.KeycloakDesiredState
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager

internal class PostgresProvisioner(private val postgresConfiguration: PostgresConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newHttpClient()

    fun provision() {
        logger.info("Provisioning Postgres...")
        provisionKeycloakSchema()
        provisionEjabberdSchema()
        logger.info("Postgres provisioning complete.")
    }

    private fun provisionKeycloakSchema() {
        val schemaName = KeycloakDesiredState.DB_SCHEMA_NAME
        createSchemaIfNotExists(schemaName)
    }

    private fun provisionEjabberdSchema() {
        val schemaName = SharedNetworkConstants.DATABASE_SCHEMA_XMPP
        createSchemaIfNotExists(schemaName)

        val alterRoleStatement = "ALTER ROLE ${postgresConfiguration.dbUser} SET search_path TO $schemaName, public"
        executeSqlStatement(alterRoleStatement)
        logger.info("User '{}' search path updated to '{}'.", postgresConfiguration.dbUser, schemaName)

        applyEjabberdSqlDefinition()

        logger.info("Ejabberd namespace '$schemaName' and tables provisioned.")
    }

    private fun createSchemaIfNotExists(schemaName: String) {
        val createSchemaStatement = "CREATE SCHEMA IF NOT EXISTS $schemaName"
        executeSqlStatement(createSchemaStatement)
        logger.info("Schema '{}' created or already exists.", schemaName)
    }

    private fun applyEjabberdSqlDefinition() {
        logger.info("Fetching and applying Ejabberd SQL schema definition...")
        val sqlUrl = "https://raw.githubusercontent.com/processone/ejabberd/25.10/sql/pg.new.sql"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(sqlUrl))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw IllegalStateException("Failed to download Ejabberd SQL definition. Status: ${response.statusCode()}")
        }

        val sqlContent = response.body()

        val statements = sqlContent.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        try {
            DriverManager.getConnection(
                postgresConfiguration.dbUrl,
                postgresConfiguration.dbUser,
                postgresConfiguration.dbPassword
            ).use { connection ->
                connection.createStatement().use { statement ->
                    statements.forEach { sql ->
                        try {
                            statement.execute(sql)
                        } catch (e: Exception) {
                            val message = e.message?.lowercase() ?: ""
                            if (!message.contains("already exists")) {
                                logger.warn("Error executing SQL statement: $sql", e)
                            }
                        }
                    }
                }
            }
        } catch (throwable: Throwable) {
            logger.error("Failed to apply Ejabberd SQL schema", throwable)
            throw IllegalStateException("Could not apply Ejabberd SQL schema", throwable)
        }
    }

    private fun executeSqlStatement(sqlStatement: String) {
        try {
            DriverManager.getConnection(
                postgresConfiguration.dbUrl,
                postgresConfiguration.dbUser,
                postgresConfiguration.dbPassword
            ).use { databaseConnection ->
                databaseConnection.createStatement().use { statement ->
                    statement.execute(sqlStatement)
                }
            }
        } catch (throwable: Throwable) {
            logger.error("Failed to execute SQL: {}", sqlStatement, throwable)
            throw IllegalStateException("Could not execute SQL statement: ${throwable.message}", throwable)
        }
    }
}