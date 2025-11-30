package com.availelabs.availe.configuration

object SharedNetworkConstants {
    const val KEYCLOAK_PUBLIC_PORT = 8180
    const val KEYCLOAK_MANAGEMENT_PORT = 9001
    const val SERVER_API_PORT = 9002
    const val POSTGRES_PORT = 5432
    const val DATABASE_SCHEMA_APPLICATION = "availe"
    const val DATABASE_SCHEMA_KEYCLOAK = "keycloak"
    const val KEYCLOAK_HEALTH_ENDPOINT_PATH = "/health/ready"
}