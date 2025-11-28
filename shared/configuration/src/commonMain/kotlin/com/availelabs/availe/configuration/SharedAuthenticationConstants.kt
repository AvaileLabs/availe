package com.availelabs.availe.configuration

object SharedAuthenticationConstants {
    const val REALM_NAME = "availe"
    const val CLIENT_ID = "availe-app"

    object Paths {
        const val OPENID_CONNECT_CERTIFICATES = "/protocol/openid-connect/certs"
        const val OPENID_CONNECT_DISCOVERY = "/.well-known/openid-configuration"
    }

    object Scopes {
        const val OPEN_ID = "openid"
        const val PROFILE = "profile"
        const val EMAIL = "email"
        const val OFFLINE_ACCESS = "offline_access"
    }
}