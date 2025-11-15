package com.availelabs.availe.domain.auth

import com.availelabs.availe.NetworkConstants

internal object AuthenticationConstants {
    const val CLIENT_ID: String = "availe-shop-app"
    const val DISCOVERY_URI: String =
        "http://${NetworkConstants.HOST_IP}:8180/realms/availe/.well-known/openid-configuration"
    const val SCOPE: String = "openid profile email"

    const val REDIRECT_URI_ANDROID: String = "com.availe.auth://callback"
    const val REDIRECT_URI_IOS: String = "com.availe.auth://callback"
    const val REDIRECT_URI_JVM: String = "http://${NetworkConstants.HOST_IP}:8888/callback"
    const val REDIRECT_URI_WEB: String = "http://${NetworkConstants.HOST_IP}:8080"

    const val POST_LOGOUT_REDIRECT_URI_ANDROID: String = "com.availe.auth://callback"
    const val POST_LOGOUT_REDIRECT_URI_IOS: String = "com.availe.auth://callback"
    const val POST_LOGOUT_REDIRECT_URI_JVM: String = "http://${NetworkConstants.HOST_IP}:8888/callback"
    const val POST_LOGOUT_REDIRECT_URI_WEB: String = "http://${NetworkConstants.HOST_IP}:8080"
}