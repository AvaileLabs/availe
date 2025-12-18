package com.availelabs.availe.domain.auth

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.configuration.SharedNetworkConstants

internal object AuthenticationConstants {
    const val CLIENT_ID: String = SharedAuthenticationConstants.CLIENT_ID
    const val DISCOVERY_URI: String =
        "http://${NetworkConstants.HOST_IP}:${SharedNetworkConstants.KEYCLOAK_PUBLIC_PORT}/realms/${SharedAuthenticationConstants.REALM_NAME}${SharedAuthenticationConstants.Paths.OPENID_CONNECT_DISCOVERY}"

    const val ACCOUNT_MANAGEMENT_URL: String =
        "http://${NetworkConstants.HOST_IP}:${SharedNetworkConstants.KEYCLOAK_PUBLIC_PORT}/realms/${SharedAuthenticationConstants.REALM_NAME}/account"

    const val REDIRECT_URI_ANDROID: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val REDIRECT_URI_IOS: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val REDIRECT_URI_JVM: String = "http://127.0.0.1:8888/callback"
    const val REDIRECT_URI_WEB: String = "http://${NetworkConstants.HOST_IP}:8080"

    const val POST_LOGOUT_REDIRECT_URI_ANDROID: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val POST_LOGOUT_REDIRECT_URI_IOS: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val POST_LOGOUT_REDIRECT_URI_JVM: String = "http://127.0.0.1:8888/callback"
    const val POST_LOGOUT_REDIRECT_URI_WEB: String = "http://${NetworkConstants.HOST_IP}:8080"
}