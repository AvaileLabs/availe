package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification

internal sealed interface OpenSearchPorts : ContainerRunSpecification.PortMapping {
    data object RestApi : OpenSearchPorts {
        override val hostPortNumber = SharedOpenSearchConstants.OPENSEARCH_PORT
        override val containerPortNumber = SharedOpenSearchConstants.OPENSEARCH_PORT
    }

    data object PerformanceAnalyzer : OpenSearchPorts {
        override val hostPortNumber = SharedOpenSearchConstants.PERFORMANCE_ANALYZER_PORT
        override val containerPortNumber = SharedOpenSearchConstants.PERFORMANCE_ANALYZER_PORT
    }
}

internal sealed interface OpenSearchDashboardsPorts : ContainerRunSpecification.PortMapping {
    data object Http : OpenSearchDashboardsPorts {
        override val hostPortNumber = SharedOpenSearchConstants.DASHBOARDS_PORT
        override val containerPortNumber = SharedOpenSearchConstants.DASHBOARDS_PORT
    }
}

internal object OpenSearchPortsList : ContainerRunSpecification.PortList {
    val RestApi = OpenSearchPorts.RestApi
    val PerformanceAnalyzer = OpenSearchPorts.PerformanceAnalyzer

    override val all = listOf(RestApi, PerformanceAnalyzer)
}

internal object OpenSearchDashboardsPortsList : ContainerRunSpecification.PortList {
    val Http = OpenSearchDashboardsPorts.Http

    override val all = listOf(Http)
}