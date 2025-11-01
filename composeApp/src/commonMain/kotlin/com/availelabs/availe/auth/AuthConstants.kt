package com.availelabs.availe.auth

internal object AuthConstants {
    const val CLIENT_ID: String = "availe-shop-app"
    const val DISCOVERY_URI: String = "http://localhost:8180/realms/availe/.well-known/openid-configuration"
    const val SCOPE: String = "openid profile email"

    const val REDIRECT_URI_ANDROID: String = "com.availe.auth://callback"
    const val REDIRECT_URI_IOS: String = "com.availe.auth://callback"
    const val REDIRECT_URI_JVM: String = "http://localhost:8888/callback"
    const val REDIRECT_URI_WEB: String = "http://localhost:8080"

    const val POST_LOGOUT_REDIRECT_URI_ANDROID: String = "com.availe.auth://callback"
    const val POST_LOGOUT_REDIRECT_URI_IOS: String = "com.availe.auth://callback"
    const val POST_LOGOUT_REDIRECT_URI_JVM: String = "http://localhost:8888/callback"
    const val POST_LOGOUT_REDIRECT_URI_WEB: String = "http://localhost:8080"
}