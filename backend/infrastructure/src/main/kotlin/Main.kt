package com.availelabs

import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.core.*
import com.availelabs.orchestration.keycloak.keycloakRunSpec
import com.availelabs.orchestration.keycloak.loadKeycloakConfig
import com.availelabs.orchestration.postgres.loadPostgresConfig
import com.availelabs.orchestration.postgres.postgresRunSpec
import com.availelabs.orchestration.redis.redisRunSpec
import com.availelabs.provisioning.PostgresProvisioner
import com.availelabs.provisioning.keycloak.KeycloakProvisioner
import com.availelabs.provisioning.keycloak.reconcilers.ClientsReconciler
import com.availelabs.provisioning.keycloak.reconcilers.UserProfileReconciler
import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory
import provisioning.keycloak.reconcilers.RealmSettingsReconciler
import kotlin.system.exitProcess

internal fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("com.availelabs.Main")
    val dockerNetworkName = "availe-network"
    val dockerClient: DockerClient = DockerClientProvider().createDockerClient()
    var exitCode = 0

    try {
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

        val postgresConfiguration = loadPostgresConfig()
        val keycloakConfiguration = loadKeycloakConfig()

        val postgresSpec = postgresRunSpec()
        val keycloakSpec = keycloakRunSpec()

        val postgresProvisioner = PostgresProvisioner(postgresConfiguration)

        val realmSettingsReconciler = RealmSettingsReconciler()
        val clientsReconciler = ClientsReconciler()
        val userProfileReconciler = UserProfileReconciler()

        val keycloakProvisioner = KeycloakProvisioner(
            keycloakConfiguration,
            realmSettingsReconciler,
            clientsReconciler,
            userProfileReconciler
        )

        val manager = InfrastructureManager(
            logger,
            containerOrchestrator,
            postgresProvisioner,
            keycloakProvisioner,
            postgresConfiguration,
            postgresSpec,
            keycloakSpec,
            redisRunSpec()
        )

        if ("--destroyAll" in args) {
            manager.teardown()
        } else {
            manager.setup()
            logger.info("System is fully provisioned and ready.")
        }

    } catch (e: Exception) {
        logger.error("An error occurred: ${e.message}", e)
        exitCode = 1
    } finally {
        logger.info("Closing Docker client.")
        dockerClient.close()
        exitProcess(exitCode)
    }
}