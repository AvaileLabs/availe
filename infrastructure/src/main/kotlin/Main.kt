package com.availelabs

import com.availelabs.containers.ContainerOrchestrator
import com.availelabs.containers.core.*
import com.availelabs.keycloak.keycloakRunSpec
import com.availelabs.keycloak.loadKeycloakConfig
import com.availelabs.postgres.loadPostgresConfig
import com.availelabs.postgres.postgresRunSpec
import com.availelabs.provisioners.ClientsReconciler
import com.availelabs.provisioners.KeycloakProvisioner
import com.availelabs.provisioners.PostgresProvisioner
import com.availelabs.provisioners.RealmSettingsReconciler
import com.availelabs.utils.ServiceWaiter
import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

internal fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("com.availelabs.Main")
    val dockerNetworkName = "availe-network"

    val dockerClient: DockerClient = DockerClientProvider().createDockerClient()
    val dockerNetworkManager = DockerNetworkManager(dockerClient, dockerNetworkName)
    val dockerVolumeManager = DockerVolumeManager(dockerClient)
    val dockerImageManager = DockerImageManager(dockerClient)
    val dockerContainerManager = DockerContainerManager(dockerClient, dockerNetworkName)

    val containerOrchestrator = ContainerOrchestrator(
        dockerNetworkManager = dockerNetworkManager,
        dockerVolumeManager = dockerVolumeManager,
        dockerImageManager = dockerImageManager,
        dockerContainerManager = dockerContainerManager
    )

    val postgresSpec = postgresRunSpec()
    val keycloakSpec = keycloakRunSpec()
    val allSpecsForTeardown = listOf(postgresSpec, keycloakSpec)

    if ("--destroyAll" in args) {
        logger.info("Starting infrastructure teardown...")
        try {
            containerOrchestrator.orchestrationTeardown(allSpecsForTeardown)
            logger.info("Infrastructure teardown complete.")
        } catch (e: Exception) {
            logger.error("An error occurred during teardown: ${e.message}", e)
        } finally {
            dockerClient.close()
        }
        exitProcess(0)
    }

    try {
        val pgConfig = loadPostgresConfig()
        val kcConfig = loadKeycloakConfig()

        logger.info("Starting Postgres container...")
        containerOrchestrator.orchestrationSetup(listOf(postgresSpec))

        ServiceWaiter.waitForPostgres(pgConfig)
        logger.info("Postgres is ready.")

        logger.info("Provisioning Postgres (creating schema)...")
        PostgresProvisioner(pgConfig).provision()

        logger.info("Starting Keycloak container...")
        containerOrchestrator.orchestrationSetup(listOf(keycloakSpec))

        ServiceWaiter.waitForKeycloak()
        logger.info("Keycloak is ready.")

        logger.info("All services are healthy. Starting Keycloak provisioning...")
        try {
            val realmSettingsReconciler = RealmSettingsReconciler()
            val clientsReconciler = ClientsReconciler()

            KeycloakProvisioner(
                kcConfig,
                realmSettingsReconciler,
                clientsReconciler
            ).provision()

            logger.info("System is fully provisioned and ready.")
        } catch (e: Exception) {
            logger.error("An error occurred during provisioning: ${e.message}", e)
        }

    } catch (e: Exception) {
        logger.error("An error occurred during orchestration or health checks: ${e.message}", e)
    } finally {
        logger.info("Closing Docker client and exiting.")
        dockerClient.close()
    }
}