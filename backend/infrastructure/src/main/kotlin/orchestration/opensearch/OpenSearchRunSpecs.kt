package com.availelabs.orchestration.opensearch

import com.availelabs.availe.NetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification.Ulimit
import java.io.File

internal fun openSearchNode1Spec(): ContainerRunSpecification<OpenSearchPortsList, OpenSearchNodeVolumesList> {
    return createOpenSearchNodeSpec(
        nodeName = InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME,
        dataVolumeName = "availe-opensearch-data1",
        certFileName = "node1.pem",
        keyFileName = "node1-key.pem",
        portMappings = OpenSearchPortsList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}

internal fun openSearchNode2Spec(): ContainerRunSpecification<ContainerRunSpecification.PortList, OpenSearchNodeVolumesList> {
    val emptyPortsList = object : ContainerRunSpecification.PortList {
        override val all = emptyList<ContainerRunSpecification.PortMapping>()
    }

    return createOpenSearchNodeSpec(
        nodeName = InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME,
        dataVolumeName = "availe-opensearch-data2",
        certFileName = "node2.pem",
        keyFileName = "node2-key.pem",
        portMappings = emptyPortsList,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}

private fun <P : ContainerRunSpecification.PortList> createOpenSearchNodeSpec(
    nodeName: String,
    dataVolumeName: String,
    certFileName: String,
    keyFileName: String,
    portMappings: P,
    logStrategy: ContainerRunSpecification.LogStrategy
): ContainerRunSpecification<P, OpenSearchNodeVolumesList> {
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/opensearch")
    val generatedCertDirectory = File(generatedConfigDirectory, "certs")
    val securityConfigDirectory = File(generatedConfigDirectory, "opensearch-security")

    val staticAssetsHash = OpenSearchSecurityHasher.calculateStaticAssetsHash(generatedConfigDirectory)

    val environmentVariables = listOf(
        "cluster.name=availe-opensearch-cluster",
        "node.name=$nodeName",
        "discovery.seed_hosts=${InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME},${InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME}",
        "cluster.initial_cluster_manager_nodes=${InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME},${InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME}",
        "bootstrap.memory_lock=true",
        "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m",
        "DISABLE_INSTALL_DEMO_CONFIG=true",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}",
        "OPENSEARCH_ASSET_HASH=$staticAssetsHash"
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
        image = InfrastructureRegistry.Images.OPENSEARCH_REPOSITORY,
        tag = InfrastructureRegistry.Images.OPENSEARCH_TAG,
        environmentVariables = environmentVariables,
        portMappings = portMappings,
        volumes = volumes,
        ulimits = ulimits,
        logStrategy = logStrategy
    )
}

internal fun openSearchDashboardsSpec(): ContainerRunSpecification<OpenSearchDashboardsPortsList, OpenSearchDashboardsVolumesList> {
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/opensearch")

    val staticAssetsHash = OpenSearchSecurityHasher.calculateStaticAssetsHash(generatedConfigDirectory)

    val environmentVariables = listOf(
        "OPENSEARCH_HOSTS=[\"https://${InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME}:${com.availelabs.availe.configuration.SharedOpenSearchConstants.OPENSEARCH_PORT}\",\"https://${InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME}:${com.availelabs.availe.configuration.SharedOpenSearchConstants.OPENSEARCH_PORT}\"]",
        "DISABLE_SECURITY_DASHBOARDS_PLUGIN=false",
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}",
        "OPENSEARCH_ASSET_HASH=$staticAssetsHash"
    )

    val volumes = OpenSearchDashboardsVolumesList(
        configHostPath = File(generatedConfigDirectory, "opensearch_dashboards.yml").absolutePath
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.OPENSEARCH_DASHBOARDS_NAME,
        image = InfrastructureRegistry.Images.OPENSEARCH_DASHBOARDS_REPOSITORY,
        tag = InfrastructureRegistry.Images.OPENSEARCH_DASHBOARDS_TAG,
        environmentVariables = environmentVariables,
        portMappings = OpenSearchDashboardsPortsList,
        volumes = volumes,
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE
    )
}