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
import java.util.concurrent.TimeUnit

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
    private val dockerContainerManager: DockerContainerManager,
    private val serviceWaiter: ServiceWaiter
) {
    private val allRunSpecificationsForTeardown = listOf(
        openSearchDashboardsRunSpecification,
        openSearchNode2RunSpecification,
        openSearchNode1RunSpecification,
        ejabberdRunSpecification,
        keycloakRunSpecification,
        postgresRunSpecification
    )

    private val containerRunSpecificationByContainerName: Map<String, ContainerRunSpecification<*, *>> = listOf(
        postgresRunSpecification,
        keycloakRunSpecification,
        ejabberdRunSpecification,
        openSearchNode1RunSpecification,
        openSearchNode2RunSpecification,
        openSearchDashboardsRunSpecification
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
                stepName = "Postgres Database",
                containerNameForLogs = postgresRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(postgresRunSpecification))
                serviceWaiter.waitForPostgres(postgresConfiguration)
                postgresProvisioner.provision()
            }

            executeProvisioningStep(
                stepName = "OpenSearch Cluster",
                containerNameForLogs = openSearchNode1RunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(
                    listOf(
                        openSearchNode1RunSpecification,
                        openSearchNode2RunSpecification
                    )
                )

                val openSearchInitializer = OpenSearchInitializer(dockerContainerManager)
                logger.info("Waiting 20s for OpenSearch Transport Layer to open before initializing security...")
                Thread.sleep(20000)

                openSearchInitializer.initializeSecurity()
                serviceWaiter.waitForOpenSearch(openSearchConfig)
            }

            executeProvisioningStep(
                stepName = "OpenSearch Dashboards",
                containerNameForLogs = openSearchDashboardsRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(openSearchDashboardsRunSpecification))
            }

            executeProvisioningStep(
                stepName = "Keycloak Identity Service",
                containerNameForLogs = keycloakRunSpecification.name
            ) {
                containerOrchestrator.orchestrationSetup(listOf(keycloakRunSpecification))
                serviceWaiter.waitForKeycloak()
                keycloakProvisioner.provision()
            }

            executeProvisioningStep(
                stepName = "Ejabberd Messaging Service",
                containerNameForLogs = ejabberdRunSpecification.name,
                performZombieCheck = true
            ) {
                generateEjabberdConfigurationFiles()
                containerOrchestrator.orchestrationSetup(listOf(ejabberdRunSpecification))
                serviceWaiter.waitForEjabberd(ejabberdRunSpecification.name)
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
        containerOrchestrator.orchestrationTeardown(allRunSpecificationsForTeardown)

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