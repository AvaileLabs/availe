package com.availelabs.containers.domain

internal sealed interface ContainerPersistenceSpecification {
    val containerPath: String

    data class BindMountPersistence(
        val hostPath: String,
        override val containerPath: String,
        val readOnly: Boolean = false
    ) :
        ContainerPersistenceSpecification

    data class NamedVolumePersistence(val volumeName: String, override val containerPath: String) :
        ContainerPersistenceSpecification
}