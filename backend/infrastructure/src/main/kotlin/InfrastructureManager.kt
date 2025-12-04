package com.availelabs

import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.PostgresProvisioner
import com.availelabs.provisioning.keycloak.KeycloakProvisioner
import com.availelabs.utils.ServiceWaiter
import org.slf4j.Logger

internal class InfrastructureManager(
    private val logger: Logger,
    private val containerOrchestrator: ContainerOrchestrator,
    private val postgresProvisioner: PostgresProvisioner,
    private val keycloakProvisioner: KeycloakProvisioner,
    private val pgConfig: PostgresConfig,
    private val postgresSpec: ContainerRunSpecification<*, *>,
    private val keycloakSpec: ContainerRunSpecification<*, *>,
    private val redisSpec: ContainerRunSpecification<*, *>
) {
    private val allSpecsForTeardown = listOf(postgresSpec, keycloakSpec)

    fun setup() {
        logger.info("Starting Postgres container...")
        containerOrchestrator.orchestrationSetup(listOf(postgresSpec))

        ServiceWaiter.waitForPostgres(pgConfig)
        logger.info("Postgres is ready.")

        logger.info("Provisioning Postgres (creating schema)...")
        postgresProvisioner.provision()

        logger.info("Starting Keycloak container...")
        containerOrchestrator.orchestrationSetup(listOf(keycloakSpec))

        ServiceWaiter.waitForKeycloak()
        logger.info("Keycloak is ready.")

        logger.info("Starting Redis container...")
        containerOrchestrator.orchestrationSetup(listOf(redisSpec))
        ServiceWaiter.waitForRedis()

        logger.info("All services are healthy. Starting Keycloak provisioning...")
        keycloakProvisioner.provision()
    }

    fun teardown() {
        logger.info("Starting infrastructure teardown...")
        containerOrchestrator.orchestrationTeardown(allSpecsForTeardown)
        logger.info("Infrastructure teardown complete.")
    }
}