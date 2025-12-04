package com.availelabs.availe.configuration

object SharedNetworkConstants {
    const val KEYCLOAK_PUBLIC_PORT = 8180
    const val KEYCLOAK_MANAGEMENT_PORT = 9001
    const val SERVER_API_PORT = 9002
    const val POSTGRES_PORT = 5432
    const val REDIS_PUBLIC_PORT = 6379
    const val MONGO_ROUTER_PORT = 27017
    const val MONGO_SHARD_PORT = 27018
    const val MONGO_CONFIG_PORT = 27019
    const val TURMS_SERVICE_ADMIN_PORT = 8510
    const val TURMS_GATEWAY_METRICS_PORT = 9510
    const val TURMS_GATEWAY_WEBSOCKET_PORT = 10510
    const val TURMS_GATEWAY_TCP_PORT = 11510
    const val TURMS_GATEWAY_UDP_PORT = 12510

    const val CONTAINER_HOSTNAME_POSTGRES = "availe-postgres"
    const val CONTAINER_HOSTNAME_KEYCLOAK = "availe-keycloak"
    const val CONTAINER_HOSTNAME_REDIS = "availe-redis"
    const val CONTAINER_HOSTNAME_MONGO_CONFIG = "availe-mongo-config"
    const val CONTAINER_HOSTNAME_MONGO_SHARD = "availe-mongo-shard"
    const val CONTAINER_HOSTNAME_MONGO_ROUTER = "availe-mongo-router"
    const val CONTAINER_HOSTNAME_TURMS_SERVICE = "availe-turms-service"
    const val CONTAINER_HOSTNAME_TURMS_GATEWAY = "availe-turms-gateway"

    const val DATABASE_SCHEMA_APPLICATION = "availe"
    const val DATABASE_SCHEMA_KEYCLOAK = "keycloak"
    const val KEYCLOAK_HEALTH_ENDPOINT_PATH = "/health/ready"
}