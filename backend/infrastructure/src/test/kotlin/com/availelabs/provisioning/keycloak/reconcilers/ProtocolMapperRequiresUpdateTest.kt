package com.availelabs.provisioning.keycloak.reconcilers

import org.keycloak.representations.idm.ProtocolMapperRepresentation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolMapperRequiresUpdateTest {
    @Test
    fun returnsFalseWhenProtocolMapperConfigMatches() {
        val existing = ProtocolMapperRepresentation().apply {
            name = "user-jid-mapper"
            protocol = "openid-connect"
            protocolMapper = "oidc-script-mapper"
            config = mapOf("claim.name" to "jid")
        }
        val desired = ProtocolMapperRepresentation().apply {
            name = "user-jid-mapper"
            protocol = "openid-connect"
            protocolMapper = "oidc-script-mapper"
            config = mapOf("claim.name" to "jid")
        }

        assertFalse(protocolMapperRequiresUpdate(existing, desired))
    }

    @Test
    fun returnsTrueWhenConfigDiffers() {
        val existing = ProtocolMapperRepresentation().apply {
            name = "user-jid-mapper"
            protocol = "openid-connect"
            protocolMapper = "oidc-usermodel-property-mapper"
            config = mapOf("claim.name" to "jid", "user.attribute" to "username")
        }
        val desired = ProtocolMapperRepresentation().apply {
            name = "user-jid-mapper"
            protocol = "openid-connect"
            protocolMapper = "oidc-script-mapper"
            config = mapOf("claim.name" to "jid", "script" to "token.setOtherClaims('jid','x')")
        }

        assertTrue(protocolMapperRequiresUpdate(existing, desired))
    }
}