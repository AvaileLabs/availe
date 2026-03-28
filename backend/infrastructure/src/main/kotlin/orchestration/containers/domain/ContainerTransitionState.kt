package com.availelabs.orchestration.containers.domain

internal sealed interface ContainerTransitionState {
    val containerIdentifier: String
    val containerName: String

    data class AlreadyRunningAndUpToDate(
        override val containerIdentifier: String,
        override val containerName: String
    ) : ContainerTransitionState

    data class FreshDeployment(
        override val containerIdentifier: String,
        override val containerName: String,
        val pendingCommitAction: () -> Unit,
        val pendingRollbackAction: () -> Unit
    ) : ContainerTransitionState

    data class UpgradePending(
        override val containerIdentifier: String,
        override val containerName: String,
        val pendingCommitAction: () -> Unit,
        val pendingRollbackAction: () -> Unit
    ) : ContainerTransitionState
}