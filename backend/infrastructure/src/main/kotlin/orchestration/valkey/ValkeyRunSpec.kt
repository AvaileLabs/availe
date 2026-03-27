package com.availelabs.orchestration.valkey

import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.utils.ServiceWaiter

internal fun valkeyRunSpecification(
    serviceWaiter: ServiceWaiter,
    valkeyConfig: ValkeyConfig
): ContainerRunSpecification<ValkeyPortsList, ValkeyVolumesList> {
    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.VALKEY_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.VALKEY_REPOSITORY,
        tag = InfrastructureRegistry.Images.VALKEY_TAG,
        portMappings = ValkeyPortsList,
        volumes = ValkeyVolumesList,
        command = listOf("valkey-server", "--requirepass", valkeyConfig.password),
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = false,
        containerHealthCheckVerificationAction = { serviceWaiter.waitForValkey(valkeyConfig) }
    )
}