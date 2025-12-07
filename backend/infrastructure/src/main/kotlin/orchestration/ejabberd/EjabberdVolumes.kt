package com.availelabs.orchestration.ejabberd

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface EjabberdVolumes : ContainerRunSpecification.VolumeMount {
    data class Config(val hostFilePath: String) : EjabberdVolumes {
        override val hostSource = hostFilePath
        override val containerPath = "/opt/ejabberd/conf/ejabberd.yml"
        override val isBindMount = true
    }

    data class ServerPem(val hostFilePath: String) : EjabberdVolumes {
        override val hostSource = hostFilePath
        override val containerPath = "/opt/ejabberd/conf/server.pem"
        override val isBindMount = true
    }

    data class KeycloakJwk(val hostFilePath: String) : EjabberdVolumes {
        override val hostSource = hostFilePath
        override val containerPath = "/opt/ejabberd/conf/keycloak.jwk"
        override val isBindMount = true
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

    data object Upload : EjabberdVolumes {
        override val hostSource = "availe-ejabberd-upload"
        override val containerPath = "/opt/ejabberd/upload"
        override val isBindMount = false
    }
    
    data object Root : EjabberdVolumes {
        override val hostSource = "availe-ejabberd-root"
        override val containerPath = "/opt/ejabberd"
        override val isBindMount = false
    }
}

internal class EjabberdVolumesList(
    configHostPath: String,
    pemHostPath: String,
    jwkHostPath: String
) : ContainerRunSpecification.VolumeList {
    val config = EjabberdVolumes.Config(configHostPath)
    val pem = EjabberdVolumes.ServerPem(pemHostPath)
    val jwk = EjabberdVolumes.KeycloakJwk(jwkHostPath)
    val data = EjabberdVolumes.Data
    val logs = EjabberdVolumes.Logs
    val upload = EjabberdVolumes.Upload
    val root = EjabberdVolumes.Root

    override val all = listOf(config, pem, jwk, data, logs, upload, root)
}