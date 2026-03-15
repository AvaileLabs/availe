package com.availelabs.availe.network

import com.availelabs.availe.NetworkConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientNetworkEndpointJvmTest {
    @Test
    fun localDesktopDevelopmentUsesLoopbackHost() {
        val endpointSet = createClientNetworkEndpointSet()
        val expectedHost = if (NetworkConstants.USE_LOCAL_IP) {
            "localhost"
        } else {
            NetworkConstants.HOST_IP
        }

        assertEquals(expectedHost, endpointSet.httpEndpoint.hostAddress.value)
        assertEquals(expectedHost, endpointSet.xmppEndpoint.hostAddress.value)
        assertEquals(expectedHost, endpointSet.keycloakEndpoint.hostAddress.value)
    }
}