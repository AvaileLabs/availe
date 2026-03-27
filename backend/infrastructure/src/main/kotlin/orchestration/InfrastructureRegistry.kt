package com.availelabs.orchestration

internal object InfrastructureRegistry {
    object Images {
        const val POSTGRES_REPOSITORY = "docker.io/library/postgres"
        const val POSTGRES_TAG = "18"

        const val KEYCLOAK_REPOSITORY = "quay.io/keycloak/keycloak"
        const val KEYCLOAK_TAG = "26.0.7"

        const val EJABBERD_REPOSITORY = "ghcr.io/processone/ejabberd"
        const val EJABBERD_TAG = "25.10"

        const val VALKEY_REPOSITORY = "valkey/valkey"
        const val VALKEY_TAG = "9.0.1"

        const val OPENSEARCH_REPOSITORY = "opensearchproject/opensearch"
        const val OPENSEARCH_TAG = "3.3.2"

        const val OPENSEARCH_DASHBOARDS_REPOSITORY = "opensearchproject/opensearch-dashboards"
        const val OPENSEARCH_DASHBOARDS_TAG = "3.3.0"
    }

    object Topology {
        const val POSTGRES_MAIN_CONTAINER_NAME = "availe-postgres"
        const val POSTGRES_KEYCLOAK_CONTAINER_NAME = "availe-postgres-keycloak"
        const val POSTGRES_EJABBERD_CONTAINER_NAME = "availe-postgres-ejabberd"
        const val KEYCLOAK_CONTAINER_NAME = "availe-keycloak"
        const val EJABBERD_CONTAINER_NAME = "availe-ejabberd"
        const val VALKEY_CONTAINER_NAME = "availe-valkey"
        const val OPENSEARCH_NODE_1_NAME = "opensearch-node1"
        const val OPENSEARCH_NODE_2_NAME = "opensearch-node2"
        const val OPENSEARCH_DASHBOARDS_NAME = "opensearch-dashboards"

        const val DATABASE_SCHEMA_APPLICATION = "availe"
        const val DATABASE_SCHEMA_KEYCLOAK = "public"
        const val DATABASE_SCHEMA_XMPP = "public"

        const val EJABBERD_DATABASE_NAME = "availe_xmpp"
        const val KEYCLOAK_DATABASE_NAME = "keycloak"
    }
}