package com.availelabs.containers.domain

import com.github.dockerjava.api.model.RestartPolicy

internal data class ContainerRunSpecification(
    val name: String,
    val image: String,
    val tag: String,
    val environmentVariables: List<String>,
    val portMappings: List<Pair<Int, Int>>,
    val persistenceSpecifications: List<ContainerPersistenceSpecification> = emptyList(),
    val restartPolicy: RestartPolicy = RestartPolicy.unlessStoppedRestart(),
    val command: List<String> = emptyList()
)