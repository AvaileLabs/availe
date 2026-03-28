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
import com.availelabs.orchestration.postgresejabberd.loadPostgresEjabberdConfig
import com.availelabs.orchestration.postgresejabberd.postgresEjabberdRunSpecification
import com.availelabs.orchestration.postgreskeycloak.loadPostgresKeycloakConfig
import com.availelabs.orchestration.postgreskeycloak.postgresKeycloakRunSpecification
import com.availelabs.orchestration.valkey.loadValkeyConfig
import com.availelabs.orchestration.valkey.valkeyRunSpecification
import com.availelabs.provisioning.PostgresProvisioner
import com.availelabs.provisioning.keycloak.KeycloakProvisioner
import com.availelabs.provisioning.keycloak.reconcilers.ClientsReconciler
import com.availelabs.provisioning.keycloak.reconcilers.UserProfileReconciler
import com.availelabs.utils.ServiceWaiter
import org.slf4j.LoggerFactory
import provisioning.keycloak.reconcilers.RealmSettingsReconciler
import kotlin.system.exitProcess

internal fun main(args: Array<String>) {
    val mainLogger = LoggerFactory.getLogger("com.availelabs.Main")
    val dockerNetworkName = "availe-network"
    val dockerClient = DockerClientProvider().createDockerClient()
    var exitCode = 0

    mainLogger.info("Initializing Infrastructure Manager...")

    try {
        val hostSystemConfiguration = loadHostSystemConfig()
        val hostSystemManager = HostSystemManager(
            LoggerFactory.getLogger(HostSystemManager::class.java),
            hostSystemConfiguration
        )

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
        val postgresKeycloakConfiguration = loadPostgresKeycloakConfig()
        val postgresEjabberdConfiguration = loadPostgresEjabberdConfig()
        val keycloakConfiguration = loadKeycloakConfig()
        val openSearchConfig = loadOpenSearchConfig()
        val valkeyConfig = loadValkeyConfig()

        val serviceWaiter = ServiceWaiter(dockerContainerManager)

        val postgresRunSpecification = postgresRunSpec(postgresConfiguration)
        val postgresKeycloakRunSpecification =
            postgresKeycloakRunSpecification(postgresKeycloakConfiguration)
        val postgresEjabberdRunSpecification =
            postgresEjabberdRunSpecification(postgresEjabberdConfiguration)
        val keycloakRunSpecification =
            keycloakRunSpec(keycloakConfiguration, postgresKeycloakConfiguration)
        val ejabberdRunSpecification = ejabberdRunSpec()
        val openSearchNode1RunSpecification = openSearchNode1Spec()
        val openSearchNode2RunSpecification = openSearchNode2Spec()
        val openSearchDashboardsRunSpecification = openSearchDashboardsSpec()
        val valkeyRunSpecification = valkeyRunSpecification(valkeyConfig)

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
            logger = mainLogger,
            containerOrchestrator = containerOrchestrator,
            postgresProvisioner = postgresProvisioner,
            keycloakProvisioner = keycloakProvisioner,
            hostSystemManager = hostSystemManager,
            postgresConfiguration = postgresConfiguration,
            postgresKeycloakConfiguration = postgresKeycloakConfiguration,
            postgresEjabberdConfiguration = postgresEjabberdConfiguration,
            openSearchConfig = openSearchConfig,
            valkeyConfig = valkeyConfig,
            postgresRunSpecification = postgresRunSpecification,
            postgresKeycloakRunSpecification = postgresKeycloakRunSpecification,
            postgresEjabberdRunSpecification = postgresEjabberdRunSpecification,
            keycloakRunSpecification = keycloakRunSpecification,
            ejabberdRunSpecification = ejabberdRunSpecification,
            openSearchNode1RunSpecification = openSearchNode1RunSpecification,
            openSearchNode2RunSpecification = openSearchNode2RunSpecification,
            openSearchDashboardsRunSpecification = openSearchDashboardsRunSpecification,
            valkeyRunSpecification = valkeyRunSpecification,
            dockerContainerManager = dockerContainerManager,
            serviceWaiter = serviceWaiter
        )

        if ("--destroyAll" in args) {
            mainLogger.info("Command received: Destroy All Infrastructure")
            infrastructureManager.teardown()
        } else {
            mainLogger.info("Command received: Setup Infrastructure")
            infrastructureManager.setup()
            mainLogger.info("System is fully provisioned and ready.")
        }

    } catch (exception: Exception) {
        mainLogger.error("An fatal error occurred during execution: ${exception.message}", exception)
        exitCode = 1
    } finally {
        mainLogger.info("Shutting down... Closing Docker client connection.")
        try {
            dockerClient.close()
        } catch (e: Exception) {
            mainLogger.warn("Error closing Docker client: ${e.message}")
        }
        exitProcess(exitCode)
    }
}