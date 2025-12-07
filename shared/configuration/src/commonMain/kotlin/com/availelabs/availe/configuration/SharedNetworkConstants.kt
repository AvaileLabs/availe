package com.availelabs.availe.configuration

object SharedNetworkConstants {
    const val KEYCLOAK_PUBLIC_PORT = 8180
    const val KEYCLOAK_MANAGEMENT_PORT = 9001
    const val SERVER_API_PORT = 9002
    const val POSTGRES_PORT = 5432

    const val EJABBERD_CLIENT_PORT = 5222
    const val EJABBERD_ADMIN_PORT = 5280
    const val EJABBERD_HOST = "localhost"
    const val XMPP_DOMAIN = "localhost"

    const val DATABASE_SCHEMA_APPLICATION = "availe"
    const val DATABASE_SCHEMA_KEYCLOAK = "keycloak"
    const val DATABASE_SCHEMA_XMPP = "availe_xmpp"

    const val KEYCLOAK_HEALTH_ENDPOINT_PATH = "/health/ready"
}