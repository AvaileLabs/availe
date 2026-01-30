package com.availelabs.availe.configuration

import com.availelabs.availe.NetworkConstants

object SharedNetworkConstants {
    const val KEYCLOAK_PUBLIC_PORT = 8180
    const val KEYCLOAK_MANAGEMENT_PORT = 9001
    const val SERVER_API_PORT = 9002
    const val POSTGRES_PORT = 5432

    const val EJABBERD_CLIENT_PORT = 5222
    const val EJABBERD_ADMIN_PORT = 5280
    const val EJABBERD_HOST = NetworkConstants.XMPP_DOMAIN
    const val XMPP_DOMAIN = NetworkConstants.XMPP_DOMAIN

    const val EJABBERD_POSTGRES_PORT = 5433
    const val EJABBERD_DB_HOST = "availe-postgres-ejabberd"
    const val EJABBERD_DB_NAME = "availe_xmpp"

    const val KEYCLOAK_POSTGRES_PORT = 5434
    const val KEYCLOAK_DB_HOST = "availe-postgres-keycloak"
    const val KEYCLOAK_DB_NAME = "keycloak"

    const val VALKEY_PORT = 6379
    const val VALKEY_HOST = "availe-valkey"

    const val DATABASE_SCHEMA_APPLICATION = "availe"
    const val DATABASE_SCHEMA_KEYCLOAK = "public"
    const val DATABASE_SCHEMA_XMPP = "public"

    const val KEYCLOAK_HEALTH_ENDPOINT_PATH = "/health/ready"
}