package com.availelabs.orchestration.containers.domain

internal data class DeploymentGroup(
    val deploymentGroupName: String,
    val containerRunSpecifications: List<ContainerRunSpecification<*, *>>,
    val groupHealthCheckVerificationAction: (() -> Unit)? = null
)