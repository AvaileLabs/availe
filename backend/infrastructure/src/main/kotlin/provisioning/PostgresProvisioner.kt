package com.availelabs.provisioning

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.keycloak.specs.KeycloakDesiredState
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.util.*

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

        // Ejabberd requires the search_path to be set explicitly for the user,
        // otherwise it defaults to 'public' and fails to find/create tables in the correct namespace.
        val alterRoleStatement = "ALTER ROLE ${postgresConfiguration.dbUser} SET search_path TO $schemaName, public"
        executeSqlStatement(alterRoleStatement)
        logger.info(
            "User '{}' search path updated to '{}'. Ejabberd will handle table creation (update_sql_schema=true).",
            postgresConfiguration.dbUser,
            schemaName
        )
    }

    private fun createSchemaIfNotExists(schemaName: String) {
        val createSchemaStatement = "CREATE SCHEMA IF NOT EXISTS $schemaName"
        executeSqlStatement(createSchemaStatement)
        logger.info("Schema '{}' created or already exists.", schemaName)
    }

    private fun executeSqlStatement(sqlStatement: String) {
        val connectionProperties = Properties().apply {
            setProperty("user", postgresConfiguration.dbUser)
            setProperty("password", postgresConfiguration.dbPassword)
        }

        try {
            DriverManager.getConnection(
                postgresConfiguration.dbUrl,
                connectionProperties
            ).use { databaseConnection ->
                databaseConnection.createStatement().use { statement ->
                    statement.execute(sqlStatement)
                }
            }
        } catch (throwable: Throwable) {
            val message = throwable.message?.lowercase() ?: ""
            if (!message.contains("already exists")) {
                logger.error("Failed to execute SQL: {}", sqlStatement, throwable)
                throw IllegalStateException("Could not execute SQL statement: ${throwable.message}", throwable)
            } else {
                logger.info("SQL execution skipped (objects already exist).")
            }
        }
    }
}