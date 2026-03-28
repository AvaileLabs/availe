package com.availelabs

import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.ContainerOrchestrator
import com.availelabs.orchestration.containers.core.DockerContainerManager
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.containers.domain.DeploymentGroup
import com.availelabs.orchestration.ejabberd.generateEjabberdConfigurationFiles
import com.availelabs.orchestration.hostsystem.HostSystemManager
import com.availelabs.orchestration.opensearch.*
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.orchestration.postgresejabberd.PostgresEjabberdConfig
import com.availelabs.orchestration.postgreskeycloak.PostgresKeycloakConfig
import com.availelabs.orchestration.valkey.ValkeyConfig
import com.availelabs.provisioning.PostgresProvisioner
import com.availelabs.provisioning.keycloak.KeycloakProvisioner
import com.availelabs.utils.ServiceWaiter
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit

internal class InfrastructureManager(
    private val logger: Logger,
    private val containerOrchestrator: ContainerOrchestrator,
    private val postgresProvisioner: PostgresProvisioner,
    private val keycloakProvisioner: KeycloakProvisioner,
    private val hostSystemManager: HostSystemManager,
    private val postgresConfiguration: PostgresConfig,
    private val postgresKeycloakConfiguration: PostgresKeycloakConfig,
    private val postgresEjabberdConfiguration: PostgresEjabberdConfig,
    private val openSearchConfig: OpenSearchConfig,
    private val valkeyConfig: ValkeyConfig,
    private val postgresRunSpecification: ContainerRunSpecification<*, *>,
    private val postgresKeycloakRunSpecification: ContainerRunSpecification<*, *>,
    private val postgresEjabberdRunSpecification: ContainerRunSpecification<*, *>,
    private val keycloakRunSpecification: ContainerRunSpecification<*, *>,
    private val ejabberdRunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchNode1RunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchNode2RunSpecification: ContainerRunSpecification<*, *>,
    private val openSearchDashboardsRunSpecification: ContainerRunSpecification<*, *>,
    private val valkeyRunSpecification: ContainerRunSpecification<*, *>,
    private val dockerContainerManager: DockerContainerManager,
    private val serviceWaiter: ServiceWaiter
) {
    private val postgresMainDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Postgres Database (Main)",
        containerRunSpecifications = listOf(postgresRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForPostgres(postgresConfiguration) }
    )

    private val postgresKeycloakDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Postgres Database (Keycloak Dedicated)",
        containerRunSpecifications = listOf(postgresKeycloakRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForPostgresKeycloak(postgresKeycloakConfiguration) }
    )

    private val postgresEjabberdDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Postgres Database (Ejabberd Dedicated)",
        containerRunSpecifications = listOf(postgresEjabberdRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForPostgresEjabberd(postgresEjabberdConfiguration) }
    )

    private val valkeyDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Valkey Cache Service",
        containerRunSpecifications = listOf(valkeyRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForValkey(valkeyConfig) }
    )

    private val openSearchClusterDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "OpenSearch Cluster",
        containerRunSpecifications = listOf(openSearchNode1RunSpecification, openSearchNode2RunSpecification),
        groupHealthCheckVerificationAction = {
            val openSearchSecurityReconciler = OpenSearchSecurityReconciler()
            openSearchSecurityReconciler.waitForClusterAvailability(60)
        }
    )

    private val openSearchDashboardsDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "OpenSearch Dashboards",
        containerRunSpecifications = listOf(openSearchDashboardsRunSpecification),
        groupHealthCheckVerificationAction = null
    )

    private val keycloakDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Keycloak Identity Service",
        containerRunSpecifications = listOf(keycloakRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForKeycloak() }
    )

    private val ejabberdDeploymentGroup = DeploymentGroup(
        deploymentGroupName = "Ejabberd Messaging Service",
        containerRunSpecifications = listOf(ejabberdRunSpecification),
        groupHealthCheckVerificationAction = { serviceWaiter.waitForEjabberd(InfrastructureRegistry.Topology.EJABBERD_CONTAINER_NAME) }
    )

    private val allDeploymentGroupsForTeardown = listOf(
        openSearchDashboardsDeploymentGroup,
        openSearchClusterDeploymentGroup,
        ejabberdDeploymentGroup,
        keycloakDeploymentGroup,
        valkeyDeploymentGroup,
        postgresKeycloakDeploymentGroup,
        postgresEjabberdDeploymentGroup,
        postgresMainDeploymentGroup
    )

    private val containerRunSpecificationByContainerName: Map<String, ContainerRunSpecification<*, *>> = listOf(
        postgresRunSpecification,
        postgresKeycloakRunSpecification,
        postgresEjabberdRunSpecification,
        keycloakRunSpecification,
        ejabberdRunSpecification,
        openSearchNode1RunSpecification,
        openSearchNode2RunSpecification,
        openSearchDashboardsRunSpecification,
        valkeyRunSpecification
    ).associateBy(ContainerRunSpecification<*, *>::name)

    private data class StepResult(
        val name: String,
        val status: StepStatus,
        val durationMs: Long,
        val errorMessage: String? = null
    )

    private enum class StepStatus { SUCCESS, FAILURE }

    private val provisioningReport = mutableListOf<StepResult>()

    fun setup() {
        try {
            executeProvisioningStep("Host System Tuning") {
                hostSystemManager.setupHostSystem()
            }

            executeProvisioningStep("Asset Generation") {
                OpenSearchCertGen.generateCertificates()
                OpenSearchConfigGen.generateConfigurationFiles()
            }

            executeProvisioningStep(
                stepName = "Postgres Database (Main)",
                containerNameForLogs = postgresRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(postgresMainDeploymentGroup))
                postgresProvisioner.provision()
            }

            executeProvisioningStep(
                stepName = "Postgres Database (Keycloak Dedicated)",
                containerNameForLogs = postgresKeycloakRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(postgresKeycloakDeploymentGroup))
            }

            executeProvisioningStep(
                stepName = "Postgres Database (Ejabberd Dedicated)",
                containerNameForLogs = postgresEjabberdRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(postgresEjabberdDeploymentGroup))
            }

            executeProvisioningStep(
                stepName = "Valkey Cache Service",
                containerNameForLogs = valkeyRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(valkeyDeploymentGroup))
            }

            executeProvisioningStep(
                stepName = "OpenSearch Cluster",
                containerNameForLogs = openSearchNode1RunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(openSearchClusterDeploymentGroup))

                val openSearchInitializer = OpenSearchInitializer(dockerContainerManager)
                openSearchInitializer.initializeSecurity()
                serviceWaiter.waitForOpenSearch(openSearchConfig)
            }

            executeProvisioningStep(
                stepName = "OpenSearch Dashboards",
                containerNameForLogs = openSearchDashboardsRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(openSearchDashboardsDeploymentGroup))
            }

            executeProvisioningStep(
                stepName = "Keycloak Identity Service",
                containerNameForLogs = keycloakRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(keycloakDeploymentGroup))
                keycloakProvisioner.provision()
            }

            val ejabberdContainerName = ejabberdRunSpecification.name

            executeProvisioningStep(
                stepName = "Ejabberd Messaging Service",
                containerNameForLogs = ejabberdContainerName,
                performZombieCheck = true
            ) {
                generateEjabberdConfigurationFiles()
                containerOrchestrator.orchestrationSetup(listOf(ejabberdDeploymentGroup))
            }

            logger.info(">>> SYSTEM PROVISIONING COMPLETE <<<")

        } catch (exception: Exception) {
            logger.error("Provisioning failed: ${exception.message}")
            throw exception
        } finally {
            printProvisioningReport()
        }
    }

    fun teardown() {
        logger.info(">>> STARTING INFRASTRUCTURE TEARDOWN <<<")
        containerOrchestrator.orchestrationTeardown(allDeploymentGroupsForTeardown)

        logger.info("Cleaning up generated host artifacts...")
        cleanLocalArtifacts()

        logger.info("Reverting host system configuration...")
        hostSystemManager.teardownHostSystem()

        logger.info(">>> INFRASTRUCTURE TEARDOWN COMPLETE <<<")
    }

    private fun executeProvisioningStep(
        stepName: String,
        containerNameForLogs: String? = null,
        performZombieCheck: Boolean = false,
        action: () -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        logger.info(">>> STARTING STEP: $stepName <<<")
        try {
            action()

            if (performZombieCheck && containerNameForLogs != null) {
                val hasErrors = dockerContainerManager.checkContainerForErrors(containerNameForLogs)
                if (hasErrors) {
                    throw IllegalStateException("Container '$containerNameForLogs' is running but logged fatal errors (Zombie state).")
                }
            }

            provisioningReport.add(StepResult(stepName, StepStatus.SUCCESS, System.currentTimeMillis() - startTime))
            logger.info(">>> STEP COMPLETED: $stepName <<<")
        } catch (exception: Exception) {
            provisioningReport.add(
                StepResult(
                    stepName,
                    StepStatus.FAILURE,
                    System.currentTimeMillis() - startTime,
                    exception.message
                )
            )
            logger.error(">>> STEP FAILED: $stepName <<<")

            if (containerNameForLogs != null) {
                if (shouldDumpBufferedLogs(containerNameForLogs)) {
                    dockerContainerManager.dumpLogsForContainer(containerNameForLogs)
                }

                if (containerNameForLogs == openSearchNode1RunSpecification.name) {
                    val openSearchSecondNodeContainerName = openSearchNode2RunSpecification.name
                    if (shouldDumpBufferedLogs(openSearchSecondNodeContainerName)) {
                        dockerContainerManager.dumpLogsForContainer(openSearchSecondNodeContainerName)
                    }
                }
            }
            throw exception
        }
    }

    private fun shouldDumpBufferedLogs(containerName: String): Boolean {
        val containerRunSpecification = containerRunSpecificationByContainerName[containerName]
        val containerLogStrategy = containerRunSpecification?.logStrategy
        return containerLogStrategy != ContainerRunSpecification.LogStrategy.STREAM_ALWAYS
    }

    private fun printProvisioningReport() {
        val border = "+-------------------------------------+----------+------------+"
        logger.info("\n")
        logger.info("PROVISIONING REPORT")
        logger.info(border)
        logger.info(String.format("| %-35s | %-8s | %-10s |", "Step Name", "Status", "Duration"))
        logger.info(border)

        provisioningReport.forEach { result ->
            val statusIcon = if (result.status == StepStatus.SUCCESS) "SUCCESS" else "FAILED"
            val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(result.durationMs)
            logger.info(
                String.format(
                    "| %-35s | %-8s | %-9ss |",
                    result.name,
                    statusIcon,
                    durationSeconds
                )
            )
        }
        logger.info(border)

        val failedStep = provisioningReport.find { it.status == StepStatus.FAILURE }
        if (failedStep != null) {
            logger.error("Failure Reason (${failedStep.name}): ${failedStep.errorMessage}")
        }
        logger.info("\n")
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