package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class KeycloakDesiredStateJidMapperTest {
    @Test
    fun jidMapperIsAttributeBasedAndExposesJidClaim() {
        val client = KeycloakDesiredState.CLIENTS.single { it.clientId == KeycloakClientIds.APP }
        val protocolMapper = client.protocolMappers.single { it.name == "user-jid-attribute-mapper" }

        assertEquals("oidc-usermodel-attribute-mapper", protocolMapper.protocolMapper)
        assertEquals("jid", protocolMapper.config["user.attribute"])
        assertEquals(SharedAuthenticationConstants.Claims.JID, protocolMapper.config["claim.name"])
    }
}