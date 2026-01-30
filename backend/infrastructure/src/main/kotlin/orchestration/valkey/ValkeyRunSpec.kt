package com.availelabs.orchestration.valkey

import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal fun valkeyRunSpecification(): ContainerRunSpecification<ValkeyPortsList, ValkeyVolumesList> {
    val valkeyConfiguration = loadValkeyConfig()

    return ContainerRunSpecification(
        name = SharedNetworkConstants.VALKEY_HOST,
        image = "valkey/valkey",
        tag = "9.0.1",
        portMappings = ValkeyPortsList,
        volumes = ValkeyVolumesList,
        command = listOf("valkey-server", "--requirepass", valkeyConfiguration.password),
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}