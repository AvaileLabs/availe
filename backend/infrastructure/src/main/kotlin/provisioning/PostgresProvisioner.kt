package com.availelabs.provisioning

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.keycloak.specs.KeycloakDesiredState
import org.slf4j.LoggerFactory
import java.sql.DriverManager

internal class PostgresProvisioner(private val postgresConfiguration: PostgresConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        logger.info("Ejabberd namespace '$schemaName' created. Tables will be created automatically by the ejabberd container.")
    }

    private fun createSchemaIfNotExists(schemaName: String) {
        val createSchemaStatement = "CREATE SCHEMA IF NOT EXISTS $schemaName"
        try {
            DriverManager.getConnection(
                postgresConfiguration.dbUrl,
                postgresConfiguration.dbUser,
                postgresConfiguration.dbPassword
            ).use { databaseConnection ->
                databaseConnection.createStatement().use { statement ->
                    statement.execute(createSchemaStatement)
                }
            }
            logger.info("Schema '{}' created or already exists.", schemaName)
        } catch (throwable: Throwable) {
            logger.error("Failed to create '$schemaName' schema: ${throwable.message}", throwable)
            throw IllegalStateException("Could not create schema '$schemaName'", throwable)
        }
    }
}