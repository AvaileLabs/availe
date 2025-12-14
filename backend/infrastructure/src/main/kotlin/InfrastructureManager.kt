package com.availelabs

import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.core.DockerContainerManager
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.ejabberd.generateEjabberdConfigurationFiles
import com.availelabs.orchestration.hostsystem.HostSystemManager
import com.availelabs.orchestration.opensearch.OpenSearchCertGen
import com.availelabs.orchestration.opensearch.OpenSearchConfig
import com.availelabs.orchestration.opensearch.OpenSearchConfigGen
import com.availelabs.orchestration.opensearch.OpenSearchInitializer
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.provisioning.PostgresProvisioner
import com.availelabs.provisioning.keycloak.KeycloakProvisioner
import com.availelabs.utils.ServiceWaiter
import org.slf4j.Logger
import java.io.File

internal class InfrastructureManager(
    private val logger: Logger,
    private val containerOrchestrator: ContainerOrchestrator,
    private val postgresProvisioner: PostgresProvisioner,
    private val keycloakProvisioner: KeycloakProvisioner,
    private val hostSystemManager: HostSystemManager,
    private val postgresConfiguration: PostgresConfig,
    private val openSearchConfig: OpenSearchConfig,
    private val postgresRunSpecification: ContainerRunSpecification<*, *>,
    private val keycloakRunSpecification: ContainerRunSpecification<*, *>,
    private val ejabberdRunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchNode1RunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchNode2RunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchDashboardsRunSpecification: ContainerRunSpecification<*, *>,
    private val dockerContainerManager: DockerContainerManager
) {
    private val allRunSpecificationsForTeardown = listOf(
        openSearchDashboardsRunSpecification,
        openSearchNode2RunSpecification,
        openSearchNode1RunSpecification,
        ejabberdRunSpecification,
        keycloakRunSpecification,
        postgresRunSpecification
    )

    fun setup() {
        logger.info(">>> STEP 1/6: HOST SYSTEM TUNING <<<")
        hostSystemManager.setupHostSystem()

        logger.info(">>> STEP 2/6: ASSET GENERATION <<<")
        logger.info("Generating OpenSearch certificates and configuration...")
        OpenSearchCertGen.generateCertificates()
        OpenSearchConfigGen.generateConfigurationFiles()

        logger.info(">>> STEP 3/6: DATABASE LAYER <<<")
        logger.info("Starting Postgres container...")
        containerOrchestrator.orchestrationSetup(listOf(postgresRunSpecification))
        ServiceWaiter.waitForPostgres(postgresConfiguration)
        logger.info("Postgres is ready.")

        logger.info("Provisioning Postgres (creating schema)...")
        postgresProvisioner.provision()

        logger.info(">>> STEP 4/6: SEARCH LAYER <<<")
        logger.info("Starting OpenSearch Cluster (Nodes 1 & 2)...")
        containerOrchestrator.orchestrationSetup(
            listOf(
                openSearchNode1RunSpecification,
                openSearchNode2RunSpecification
            )
        )

        // Force Initialization of Security (Bypassing permission issues)
        val openSearchInitializer = OpenSearchInitializer(dockerContainerManager)

        logger.info("Waiting 20s for OpenSearch Transport Layer to open before initializing security...")
        Thread.sleep(20000)

        openSearchInitializer.initializeSecurity()

        logger.info("Waiting for OpenSearch Cluster to form...")
        ServiceWaiter.waitForOpenSearch(openSearchConfig)
        logger.info("OpenSearch Cluster is ready.")

        logger.info("Starting OpenSearch Dashboards...")
        containerOrchestrator.orchestrationSetup(listOf(openSearchDashboardsRunSpecification))

        logger.info(">>> STEP 5/6: APPLICATION/IDENTITY LAYER <<<")
        logger.info("Starting Keycloak container...")
        containerOrchestrator.orchestrationSetup(listOf(keycloakRunSpecification))
        ServiceWaiter.waitForKeycloak()
        logger.info("Keycloak is ready.")

        logger.info("Provisioning Keycloak...")
        keycloakProvisioner.provision()

        logger.info(">>> STEP 6/6: MESSAGING LAYER <<<")
        logger.info("Fetching Keycloak keys and generating Ejabberd configuration...")
        generateEjabberdConfigurationFiles()

        logger.info("Starting Ejabberd container...")
        containerOrchestrator.orchestrationSetup(listOf(ejabberdRunSpecification))
        ServiceWaiter.waitForEjabberd()
        logger.info("Ejabberd is ready.")

        logger.info(">>> SYSTEM PROVISIONING COMPLETE <<<")
    }

    fun teardown() {
        logger.info(">>> STARTING INFRASTRUCTURE TEARDOWN <<<")
        containerOrchestrator.orchestrationTeardown(allRunSpecificationsForTeardown)

        logger.info("Cleaning up generated host artifacts...")
        cleanLocalArtifacts()

        logger.info("Reverting host system configuration...")
        hostSystemManager.teardownHostSystem()

        logger.info(">>> INFRASTRUCTURE TEARDOWN COMPLETE <<<")
    }

    private fun cleanLocalArtifacts() {
        val projectRootDirectory = System.getProperty("user.dir")
        val generatedDirectory = File(projectRootDirectory, "build/generated")
        if (generatedDirectory.exists()) {
            val deleted = generatedDirectory.deleteRecursively()
            if (deleted) {
                logger.info("Deleted generated configuration files at ${generatedDirectory.absolutePath}")
            } else {
                logger.warn("Failed to delete generated configuration files at ${generatedDirectory.absolutePath}")
            }
        }
    }
}