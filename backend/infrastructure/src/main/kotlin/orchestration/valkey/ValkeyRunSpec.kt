package com.availelabs.orchestration.valkey

import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun valkeyRunSpecification(): ContainerRunSpecification<ValkeyPortsList, ValkeyVolumesList> {
    val valkeyConfiguration = loadValkeyConfig()

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.VALKEY_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.VALKEY_REPOSITORY,
        tag = InfrastructureRegistry.Images.VALKEY_TAG,
        portMappings = ValkeyPortsList,
        volumes = ValkeyVolumesList,
        command = listOf("valkey-server", "--requirepass", valkeyConfiguration.password),
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}