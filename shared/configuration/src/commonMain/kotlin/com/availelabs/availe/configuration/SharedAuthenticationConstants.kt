package com.availelabs.availe.configuration

object SharedAuthenticationConstants {
    const val REALM_NAME = "availe"
    const val CLIENT_ID = "availe-app"
    const val REDIRECT_SCHEME = "com.availe.auth"

    object Paths {
        const val OPENID_CONNECT_CERTIFICATES = "/protocol/openid-connect/certs"
        const val OPENID_CONNECT_DISCOVERY = "/.well-known/openid-configuration"
    }

    object Scopes {
        const val OPEN_ID = "openid"
        const val PROFILE = "profile"
        const val EMAIL = "email"
        const val OFFLINE_ACCESS = "offline_access"
        const val ROLES = "roles"
        const val WEB_ORIGINS = "web-origins"
    }

    object Claims {
        const val SUBJECT = "sub"
        const val ISSUER = "iss"
        const val EMAIL = "email"
        const val USER_ID = "user_id"
    }

    object GrantTypes {
        const val AUTHORIZATION_CODE = "authorization_code"
        const val REFRESH_TOKEN = "refresh_token"
        const val PASSWORD = "password"
    }
}