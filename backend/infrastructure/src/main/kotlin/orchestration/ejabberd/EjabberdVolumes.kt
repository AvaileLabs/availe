package com.availelabs.orchestration.ejabberd

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface EjabberdVolumes : ContainerRunSpecification.VolumeMount {
    data class Config(val hostFilePath: String) : EjabberdVolumes {
        override val hostSource = hostFilePath
        override val containerPath = "/opt/ejabberd/conf/ejabberd.yml"
        override val isBindMount = true
    }

    data object Opt : EjabberdVolumes {
        override val hostSource = "availe-ejabberd-opt"
        override val containerPath = "/opt/ejabberd"
        override val isBindMount = false
    }

    data object Data : EjabberdVolumes {
        override val hostSource = "availe-ejabberd-data"
        override val containerPath = "/opt/ejabberd/database"
        override val isBindMount = false
    }

    data object Logs : EjabberdVolumes {
        override val hostSource = "availe-ejabberd-logs"
        override val containerPath = "/opt/ejabberd/logs"
        override val isBindMount = false
    }
}

internal class EjabberdVolumesList(private val configHostPath: String) : ContainerRunSpecification.VolumeList {
    val Config = EjabberdVolumes.Config(configHostPath)
    val Opt = EjabberdVolumes.Opt
    val Data = EjabberdVolumes.Data
    val Logs = EjabberdVolumes.Logs

    override val all = listOf(Config, Opt, Data, Logs)
}