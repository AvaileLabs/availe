package com.availelabs.availe.configuration

object SharedOpenSearchConstants {
    // Versioning - Decoupled as Dashboards releases may lag behind Core releases
    const val OPENSEARCH_VERSION = "3.3.2"
    const val DASHBOARDS_VERSION = "3.3.0"

    // Ports
    const val OPENSEARCH_PORT = 9200
    const val PERFORMANCE_ANALYZER_PORT = 9600
    const val DASHBOARDS_PORT = 5601

    // Node Names
    const val NODE_1_NAME = "opensearch-node1"
    const val NODE_2_NAME = "opensearch-node2"
    const val DASHBOARDS_NAME = "opensearch-dashboards"
}