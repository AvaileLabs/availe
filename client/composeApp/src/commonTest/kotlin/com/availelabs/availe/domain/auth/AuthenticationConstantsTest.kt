package com.availelabs.availe.domain.auth

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticationConstantsTest {
    @Test
    fun accountManagementUrlSharesOriginWithDiscoveryUrl() {
        val discoveryUrl = Url(AuthenticationConstants.DISCOVERY_URI)
        val accountManagementUrl = Url(AuthenticationConstants.ACCOUNT_MANAGEMENT_URL)

        assertEquals(discoveryUrl.protocol, accountManagementUrl.protocol)
        assertEquals(discoveryUrl.host, accountManagementUrl.host)
        assertEquals(discoveryUrl.port, accountManagementUrl.port)
        assertTrue(
            accountManagementUrl.encodedPath.endsWith(
                "/realms/${SharedAuthenticationConstants.REALM_NAME}/account"
            )
        )
    }
}