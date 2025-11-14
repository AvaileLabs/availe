package com.availelabs.availe.testutils

import org.flywaydb.core.Flyway
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection

@Testcontainers
abstract class PostgresTestBase {

    protected lateinit var dsl: DSLContext
    private lateinit var connection: Connection

    companion object {
        @Container
        @JvmStatic
        val postgresContainer: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("availe-test")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("currentSchema", "availe")

        private lateinit var baseConfig: Configuration

        @BeforeAll
        @JvmStatic
        fun setup() {
            Flyway.configure()
                .dataSource(postgresContainer.jdbcUrl, "test", "test")
                .schemas("availe")
                .defaultSchema("availe")
                .load()
                .migrate()

            val testDataSource = PGSimpleDataSource().apply {
                setURL(postgresContainer.jdbcUrl)
                user = "test"
                password = "test"
            }

            baseConfig = DefaultConfiguration()
                .set(testDataSource)
                .set(SQLDialect.POSTGRES)
                .set(Settings().withReturnDefaultOnUpdatableRecord(true))
        }
    }

    @BeforeEach
    fun setupTestTransaction() {
        connection = baseConfig.connectionProvider().acquire()!!
        connection.autoCommit = false
        dsl = DSL.using(connection, SQLDialect.POSTGRES, baseConfig.settings())
    }

    @AfterEach
    fun rollbackTestTransaction() {
        connection.rollback()
        connection.autoCommit = true
        connection.close()
    }
}