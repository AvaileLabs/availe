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

        logger.info("Applying ejabberd SQL schema to '$schemaName'...")
        val schemaSql = loadEjabberdSchemaSql()

        DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword).use { conn ->
            conn.createStatement().use { st ->
                st.execute("SET search_path TO $schemaName")
                st.execute(schemaSql)
            }
        }
        logger.info("ejabberd SQL schema applied successfully.")
    }

    private fun setDefaultSearchPathForUser() {
        val dbName = config.dbUrl.substringAfterLast('/')
        val ejabberdSchema = SharedNetworkConstants.DATABASE_SCHEMA_XMPP
        val publicSchema = "public"

        logger.info("Setting default search_path for user '${config.dbUser}' in database '$dbName' to '$ejabberdSchema, $publicSchema'...")

        val sql =
            "ALTER ROLE \"${config.dbUser}\" IN DATABASE \"$dbName\" SET search_path TO $ejabberdSchema, $publicSchema"

        try {
            DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword).use { conn ->
                conn.createStatement().use { st ->
                    st.execute(sql)
                }
            }
            logger.info("Default search_path configured successfully.")
        } catch (t: Throwable) {
            logger.error("Failed to set default search_path: ${t.message}", t)
            throw IllegalStateException("Could not configure search_path", t)
        }
    }

    private fun createSchemaIfNotExists(schemaName: String) {
        val sql = "CREATE SCHEMA IF NOT EXISTS $schemaName"
        try {
            DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword).use { conn ->
                conn.createStatement().use { st ->
                    st.execute(sql)
                }
            }
            logger.info("Schema '{}' created or already exists.", schemaName)
        } catch (t: Throwable) {
            logger.error("Failed to create '$schemaName' schema: ${t.message}", t)
            throw IllegalStateException("Could not create schema '$schemaName'", t)
        }
    }

    private fun loadEjabberdSchemaSql(): String {
        return object {}.javaClass.getResource("/ejabberd_schema.sql")
            ?.readText()
            ?: error("Could not find /ejabberd_schema.sql in resources")
    }
}