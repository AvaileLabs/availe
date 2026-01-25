package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeycloakDesiredStateJidMapperTest {
    @Test
    fun jidMapperIsScriptBasedAndEmitsLocalhostJid() {
        val client = KeycloakDesiredState.CLIENTS.single { it.clientId == KeycloakClientIds.APP }
        val mapper = client.protocolMappers.single { it.name == "user-jid-mapper" }

        assertEquals("oidc-script-mapper", mapper.protocolMapper)
        assertEquals(SharedAuthenticationConstants.Claims.JID, mapper.config["claim.name"])

        val script = mapper.config["script"]
        assertNotNull(script)
        assertTrue(script.contains("@localhost"))
    }
}