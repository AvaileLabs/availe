package com.availelabs.orchestration.valkey

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun valkeyRunSpecification(
    valkeyConfig: ValkeyConfig
): ContainerRunSpecification<ValkeyPortsList, ValkeyVolumesList> {
    return ContainerRunSpecification(
        name = SharedNetworkConstants.VALKEY_HOST,
        image = InfrastructureRegistry.Images.VALKEY_REPOSITORY,
        tag = InfrastructureRegistry.Images.VALKEY_TAG,
        portMappings = ValkeyPortsList,
        volumes = ValkeyVolumesList,
        command = listOf("valkey-server", "--requirepass", valkeyConfig.password),
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = false
    )
}