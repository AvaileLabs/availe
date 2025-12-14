package com.availelabs

import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.core.*
import com.availelabs.orchestration.ejabberd.ejabberdRunSpec
import com.availelabs.orchestration.hostsystem.HostSystemManager
import com.availelabs.orchestration.hostsystem.loadHostSystemConfig
import com.availelabs.orchestration.keycloak.keycloakRunSpec
import com.availelabs.orchestration.keycloak.loadKeycloakConfig
import com.availelabs.orchestration.opensearch.loadOpenSearchConfig
import com.availelabs.orchestration.opensearch.openSearchDashboardsSpec
import com.availelabs.orchestration.opensearch.openSearchNode1Spec
import com.availelabs.orchestration.opensearch.openSearchNode2Spec
import com.availelabs.orchestration.postgres.loadPostgresConfig
import com.availelabs.orchestration.postgres.postgresRunSpec
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

    logger.info("Initializing Infrastructure Manager...")

    try {
        val hostSystemConfiguration = loadHostSystemConfig()
        val hostSystemManager = HostSystemManager(logger, hostSystemConfiguration)

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
        val openSearchConfig = loadOpenSearchConfig()

        val postgresRunSpecification = postgresRunSpec()
        val keycloakRunSpecification = keycloakRunSpec()
        val ejabberdRunSpecification = ejabberdRunSpec()
        val openSearchNode1RunSpecification = openSearchNode1Spec()
        val openSearchNode2RunSpecification = openSearchNode2Spec()
        val openSearchDashboardsRunSpecification = openSearchDashboardsSpec()

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

        val infrastructureManager = InfrastructureManager(
            logger = logger,
            containerOrchestrator = containerOrchestrator,
            postgresProvisioner = postgresProvisioner,
            keycloakProvisioner = keycloakProvisioner,
            hostSystemManager = hostSystemManager,
            postgresConfiguration = postgresConfiguration,
            openSearchConfig = openSearchConfig,
            postgresRunSpecification = postgresRunSpecification,
            keycloakRunSpecification = keycloakRunSpecification,
            ejabberdRunSpecification = ejabberdRunSpecification,
            openSearchNode1RunSpecification = openSearchNode1RunSpecification,
            openSearchNode2RunSpecification = openSearchNode2RunSpecification,
            openSearchDashboardsRunSpecification = openSearchDashboardsRunSpecification,
            dockerContainerManager = dockerContainerManager
        )

        if ("--destroyAll" in args) {
            logger.info("Command received: Destroy All Infrastructure")
            infrastructureManager.teardown()
        } else {
            logger.info("Command received: Setup Infrastructure")
            infrastructureManager.setup()
            logger.info("System is fully provisioned and ready.")
        }

    } catch (exception: Exception) {
        logger.error("An fatal error occurred during execution: ${exception.message}", exception)
        exitCode = 1
    } finally {
        logger.info("Shutting down... Closing Docker client connection.")
        try {
            dockerClient.close()
        } catch (e: Exception) {
            logger.warn("Error closing Docker client: ${e.message}")
        }
        exitProcess(exitCode)
    }
}