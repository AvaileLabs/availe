package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification.Ulimit
import java.io.File

internal fun openSearchNode1Spec(): ContainerRunSpecification<OpenSearchPortsList, OpenSearchNodeVolumesList> {
    return createOpenSearchNodeSpec(
        nodeName = SharedOpenSearchConstants.NODE_1_NAME,
        dataVolumeName = "availe-opensearch-data1",
        certFileName = "node1.pem",
        keyFileName = "node1-key.pem",
        portMappings = OpenSearchPortsList // Expose ports on host only for Node 1
    )
}

internal fun openSearchNode2Spec(): ContainerRunSpecification<ContainerRunSpecification.PortList, OpenSearchNodeVolumesList> {
    // Node 2 is internal; we do not map its ports to the host to avoid conflicts/redundancy,
    // but it needs to be accessible on the docker network.
    val emptyPortsList = object : ContainerRunSpecification.PortList {
        override val all = emptyList<ContainerRunSpecification.PortMapping>()
    }

    return createOpenSearchNodeSpec(
        nodeName = SharedOpenSearchConstants.NODE_2_NAME,
        dataVolumeName = "availe-opensearch-data2",
        certFileName = "node2.pem",
        keyFileName = "node2-key.pem",
        portMappings = emptyPortsList
    )
}

private fun <P : ContainerRunSpecification.PortList> createOpenSearchNodeSpec(
    nodeName: String,
    dataVolumeName: String,
    certFileName: String,
    keyFileName: String,
    portMappings: P
): ContainerRunSpecification<P, OpenSearchNodeVolumesList> {
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/opensearch")
    val generatedCertDirectory = File(generatedConfigDirectory, "certs")
    val securityConfigDirectory = File(generatedConfigDirectory, "opensearch-security")

    val environmentVariables = listOf(
        "cluster.name=availe-opensearch-cluster",
        "node.name=$nodeName",
        "discovery.seed_hosts=${SharedOpenSearchConstants.NODE_1_NAME},${SharedOpenSearchConstants.NODE_2_NAME}",
        "cluster.initial_cluster_manager_nodes=${SharedOpenSearchConstants.NODE_1_NAME},${SharedOpenSearchConstants.NODE_2_NAME}",
        "bootstrap.memory_lock=true",
        "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m",
        "DISABLE_INSTALL_DEMO_CONFIG=true", // We are providing our own security config
        "OPENSEARCH_CONFIG_GENERATION_TIMESTAMP=${System.currentTimeMillis()}"
    )

    val ulimits = listOf(
        Ulimit("memlock", -1, -1),
        Ulimit("nofile", 65536, 65536)
    )

    val volumes = OpenSearchNodeVolumesList(
        configHostPath = File(generatedConfigDirectory, "opensearch.yml").absolutePath,
        rootCaHostPath = File(generatedCertDirectory, "root-ca.pem").absolutePath,
        nodeCertHostPath = File(generatedCertDirectory, certFileName).absolutePath,
        nodeKeyHostPath = File(generatedCertDirectory, keyFileName).absolutePath,
        adminCertHostPath = File(generatedCertDirectory, "admin.pem").absolutePath,
        adminKeyHostPath = File(generatedCertDirectory, "admin-key.pem").absolutePath,
        dataVolumeName = dataVolumeName,
        internalUsersHostPath = File(securityConfigDirectory, "internal_users.yml").absolutePath,
        rolesMappingHostPath = File(securityConfigDirectory, "roles_mapping.yml").absolutePath,
        securityConfigHostPath = File(securityConfigDirectory, "config.yml").absolutePath,
        rolesHostPath = File(securityConfigDirectory, "roles.yml").absolutePath,
        tenantsHostPath = File(securityConfigDirectory, "tenants.yml").absolutePath,
        nodesDnHostPath = File(securityConfigDirectory, "nodes_dn.yml").absolutePath,
        whitelistHostPath = File(securityConfigDirectory, "whitelist.yml").absolutePath,
        auditHostPath = File(securityConfigDirectory, "audit.yml").absolutePath
    )

    return ContainerRunSpecification(
        name = nodeName,
        image = "opensearchproject/opensearch",
        tag = SharedOpenSearchConstants.OPENSEARCH_VERSION,
        environmentVariables = environmentVariables,
        portMappings = portMappings,
        volumes = volumes,
        ulimits = ulimits
    )
}

internal fun openSearchDashboardsSpec(): ContainerRunSpecification<OpenSearchDashboardsPortsList, OpenSearchDashboardsVolumesList> {
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/opensearch")

    val environmentVariables = listOf(
        "OPENSEARCH_HOSTS=[\"https://${SharedOpenSearchConstants.NODE_1_NAME}:${SharedOpenSearchConstants.OPENSEARCH_PORT}\",\"https://${SharedOpenSearchConstants.NODE_2_NAME}:${SharedOpenSearchConstants.OPENSEARCH_PORT}\"]",
        "DISABLE_SECURITY_DASHBOARDS_PLUGIN=false",
        "OPENSEARCH_CONFIG_GENERATION_TIMESTAMP=${System.currentTimeMillis()}"
    )

    val volumes = OpenSearchDashboardsVolumesList(
        configHostPath = File(generatedConfigDirectory, "opensearch_dashboards.yml").absolutePath
    )

    return ContainerRunSpecification(
        name = SharedOpenSearchConstants.DASHBOARDS_NAME,
        image = "opensearchproject/opensearch-dashboards",
        tag = SharedOpenSearchConstants.DASHBOARDS_VERSION,
        environmentVariables = environmentVariables,
        portMappings = OpenSearchDashboardsPortsList,
        volumes = volumes
    )
}