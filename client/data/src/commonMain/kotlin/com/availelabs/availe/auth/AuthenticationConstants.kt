package com.availelabs.availe.auth

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.network.ClientNetworkEndpoint

internal object AuthenticationConstants {
    const val CLIENT_ID: String = SharedAuthenticationConstants.CLIENT_ID
    val DISCOVERY_URI: String
        get() = ClientNetworkEndpoint.current.keycloakEndpoint.realmBaseUrl + SharedAuthenticationConstants.Paths.OPENID_CONNECT_DISCOVERY

    val ACCOUNT_MANAGEMENT_URL: String
        get() = ClientNetworkEndpoint.current.keycloakEndpoint.realmBaseUrl + "/account"

    const val REDIRECT_URI_ANDROID: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val REDIRECT_URI_IOS: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val REDIRECT_URI_JVM: String = "http://127.0.0.1:8888/callback"
    val REDIRECT_URI_WEB: String
        get() = "http://${ClientNetworkEndpoint.current.httpEndpoint.hostAddress.value}:8080"

    const val POST_LOGOUT_REDIRECT_URI_ANDROID: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val POST_LOGOUT_REDIRECT_URI_IOS: String = "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
    const val POST_LOGOUT_REDIRECT_URI_JVM: String = "http://127.0.0.1:8888/callback"
    val POST_LOGOUT_REDIRECT_URI_WEB: String
        get() = "http://${ClientNetworkEndpoint.current.httpEndpoint.hostAddress.value}:8080"
}