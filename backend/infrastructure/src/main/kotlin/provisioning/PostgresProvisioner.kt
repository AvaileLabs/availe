package com.availelabs.provisioning

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.keycloak.specs.KeycloakDesiredState
import org.slf4j.LoggerFactory
import java.sql.DriverManager

internal class PostgresProvisioner(private val config: PostgresConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun provision() {
        logger.info("Provisioning Postgres...")
        provisionKeycloakSchema()
        provisionEjabberdSchema()
        setDefaultSearchPathForUser()
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

    private fun setDefaultSearchPathForUser() {
        val databaseName = config.dbUrl.substringAfterLast('/')
        val ejabberdSchema = SharedNetworkConstants.DATABASE_SCHEMA_XMPP
        val publicSchema = "public"

        logger.info("Setting default search_path for user '${config.dbUser}' in database '$databaseName' to '$ejabberdSchema, $publicSchema'...")

        val sql =
            "ALTER ROLE \"${config.dbUser}\" IN DATABASE \"$databaseName\" SET search_path TO $ejabberdSchema, $publicSchema"

        try {
            DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(sql)
                }
            }
            logger.info("Default search_path configured successfully.")
        } catch (throwable: Throwable) {
            logger.error("Failed to set default search_path: ${throwable.message}", throwable)
            throw IllegalStateException("Could not configure search_path", throwable)
        }
    }

    private fun createSchemaIfNotExists(schemaName: String) {
        val sql = "CREATE SCHEMA IF NOT EXISTS $schemaName"
        try {
            DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(sql)
                }
            }
            logger.info("Schema '{}' created or already exists.", schemaName)
        } catch (throwable: Throwable) {
            logger.error("Failed to create '$schemaName' schema: ${throwable.message}", throwable)
            throw IllegalStateException("Could not create schema '$schemaName'", throwable)
        }
    }
}