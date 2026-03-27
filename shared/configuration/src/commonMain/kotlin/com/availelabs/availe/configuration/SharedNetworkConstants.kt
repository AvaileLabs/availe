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

    const val KEYCLOAK_POSTGRES_PORT = 5434

    const val VALKEY_PORT = 6379

    const val KEYCLOAK_HEALTH_ENDPOINT_PATH = "/health/ready"
}