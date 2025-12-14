package com.availelabs.orchestration.opensearch

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface OpenSearchVolumes : ContainerRunSpecification.VolumeMount {
    data class RootCa(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/certs/root-ca.pem"
        override val isBindMount = true
    }

    data class Config(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch.yml"
        override val isBindMount = true
    }

    data class NodeCert(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/certs/node.pem"
        override val isBindMount = true
    }

    data class NodeKey(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/certs/node-key.pem"
        override val isBindMount = true
    }

    data class AdminCert(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/certs/admin.pem"
        override val isBindMount = true
    }

    data class AdminKey(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/certs/admin-key.pem"
        override val isBindMount = true
    }

    data class Data(val volumeName: String) : OpenSearchVolumes {
        override val hostSource = volumeName
        override val containerPath = "/usr/share/opensearch/data"
        override val isBindMount = false
    }

    data class InternalUsers(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/internal_users.yml"
        override val isBindMount = true
    }

    data class RolesMapping(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/roles_mapping.yml"
        override val isBindMount = true
    }

    data class SecurityConfig(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/config.yml"
        override val isBindMount = true
    }

    data class Roles(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/roles.yml"
        override val isBindMount = true
    }

    data class Tenants(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/tenants.yml"
        override val isBindMount = true
    }

    data class NodesDn(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/nodes_dn.yml"
        override val isBindMount = true
    }

    data class Whitelist(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/whitelist.yml"
        override val isBindMount = true
    }

    data class Audit(val hostPath: String) : OpenSearchVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch/config/opensearch-security/audit.yml"
        override val isBindMount = true
    }
}

internal sealed interface OpenSearchDashboardsVolumes : ContainerRunSpecification.VolumeMount {
    data class Config(val hostPath: String) : OpenSearchDashboardsVolumes {
        override val hostSource = hostPath
        override val containerPath = "/usr/share/opensearch-dashboards/config/opensearch_dashboards.yml"
        override val isBindMount = true
    }
}

internal class OpenSearchNodeVolumesList(
    configHostPath: String,
    rootCaHostPath: String,
    nodeCertHostPath: String,
    nodeKeyHostPath: String,
    adminCertHostPath: String,
    adminKeyHostPath: String,
    dataVolumeName: String,
    internalUsersHostPath: String,
    rolesMappingHostPath: String,
    securityConfigHostPath: String,
    rolesHostPath: String,
    tenantsHostPath: String,
    nodesDnHostPath: String,
    whitelistHostPath: String,
    auditHostPath: String
) : ContainerRunSpecification.VolumeList {
    val config = OpenSearchVolumes.Config(configHostPath)
    val rootCa = OpenSearchVolumes.RootCa(rootCaHostPath)
    val nodeCert = OpenSearchVolumes.NodeCert(nodeCertHostPath)
    val nodeKey = OpenSearchVolumes.NodeKey(nodeKeyHostPath)
    val adminCert = OpenSearchVolumes.AdminCert(adminCertHostPath)
    val adminKey = OpenSearchVolumes.AdminKey(adminKeyHostPath)
    val data = OpenSearchVolumes.Data(dataVolumeName)

    val internalUsers = OpenSearchVolumes.InternalUsers(internalUsersHostPath)
    val rolesMapping = OpenSearchVolumes.RolesMapping(rolesMappingHostPath)
    val securityConfig = OpenSearchVolumes.SecurityConfig(securityConfigHostPath)
    val roles = OpenSearchVolumes.Roles(rolesHostPath)
    val tenants = OpenSearchVolumes.Tenants(tenantsHostPath)
    val nodesDn = OpenSearchVolumes.NodesDn(nodesDnHostPath)
    val whitelist = OpenSearchVolumes.Whitelist(whitelistHostPath)
    val audit = OpenSearchVolumes.Audit(auditHostPath)

    override val all = listOf(
        config, rootCa, nodeCert, nodeKey, adminCert, adminKey, data,
        internalUsers, rolesMapping, securityConfig, roles,
        tenants, nodesDn, whitelist, audit
    )
}

internal class OpenSearchDashboardsVolumesList(
    configHostPath: String
) : ContainerRunSpecification.VolumeList {
    val config = OpenSearchDashboardsVolumes.Config(configHostPath)

    override val all = listOf(config)
}