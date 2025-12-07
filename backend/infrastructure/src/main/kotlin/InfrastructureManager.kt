package com.availelabs

import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.ejabberd.generateEjabberdConfigurationFiles
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
    private val postgresConfiguration: PostgresConfig,
    private val postgresRunSpecification: ContainerRunSpecification<*, *>,
    private val keycloakRunSpecification: ContainerRunSpecification<*, *>,
    private val ejabberdRunSpecification: ContainerRunSpecification<*, *>
) {
    private val allRunSpecificationsForTeardown = listOf(
        postgresRunSpecification,
        ejabberdRunSpecification,
        keycloakRunSpecification
    )

    fun setup() {
        logger.info("Starting Postgres container...")
        containerOrchestrator.orchestrationSetup(listOf(postgresRunSpecification))

        ServiceWaiter.waitForPostgres(postgresConfiguration)
        logger.info("Postgres is ready.")

        logger.info("Provisioning Postgres (creating schema)...")
        postgresProvisioner.provision()

        logger.info("Starting Keycloak container...")
        containerOrchestrator.orchestrationSetup(listOf(keycloakRunSpecification))

        ServiceWaiter.waitForKeycloak()
        logger.info("Keycloak is ready.")

        logger.info("Provisioning Keycloak...")
        keycloakProvisioner.provision()

        logger.info("Fetching Keycloak keys and generating Ejabberd configuration...")
        generateEjabberdConfigurationFiles()

        logger.info("Starting Ejabberd container...")
        containerOrchestrator.orchestrationSetup(listOf(ejabberdRunSpecification))

        ServiceWaiter.waitForEjabberd()
        logger.info("Ejabberd is ready.")

        logger.info("All services are healthy.")
    }

    fun teardown() {
        logger.info("Starting infrastructure teardown...")
        containerOrchestrator.orchestrationTeardown(allRunSpecificationsForTeardown)
        logger.info("Infrastructure teardown complete.")
    }
}