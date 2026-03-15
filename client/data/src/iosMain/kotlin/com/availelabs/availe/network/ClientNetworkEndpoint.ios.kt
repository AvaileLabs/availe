package com.availelabs.availe.network

import com.availelabs.availe.NetworkConstants

internal actual fun createClientNetworkEndpointSet(): ClientNetworkEndpointSet {
    return createClientNetworkEndpointSet(HostAddress(NetworkConstants.HOST_IP))
}