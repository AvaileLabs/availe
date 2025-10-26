package com.availelabs.provisioners

import com.availelabs.postgres.PostgresConfig
import com.availelabs.provisioners.spec.KeycloakDesiredState
import org.slf4j.LoggerFactory
import java.sql.DriverManager

internal class PostgresProvisioner(private val config: PostgresConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun provision() {
        logger.info("Provisioning Postgres...")
        val schemaName = KeycloakDesiredState.DB_SCHEMA_NAME
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
}