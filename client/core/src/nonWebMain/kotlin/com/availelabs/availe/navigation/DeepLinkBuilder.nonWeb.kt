package com.availelabs.availe.navigation

import com.availelabs.availe.NetworkConstants

actual object DeepLinkBuilder {
    actual fun build(route: DeepLinkRoute): String {
        val host = NetworkConstants.HOST_IP
        val port = "8080"
        val cleanPath = route.relativePath.trimStart('/')
        return "http://$host:$port/$cleanPath"
    }
}