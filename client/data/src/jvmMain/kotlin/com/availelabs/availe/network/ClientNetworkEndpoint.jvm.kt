package com.availelabs.availe.network

import com.availelabs.availe.NetworkConstants

internal actual fun createClientNetworkEndpointSet(): ClientNetworkEndpointSet {
    val hostAddress = if (NetworkConstants.USE_LOCAL_IP) {
        HostAddress("localhost")
    } else {
        HostAddress(NetworkConstants.HOST_IP)
    }

    return createClientNetworkEndpointSet(hostAddress)
}