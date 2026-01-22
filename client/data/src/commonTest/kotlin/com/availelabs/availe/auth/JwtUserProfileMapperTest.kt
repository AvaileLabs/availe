package com.availelabs.availe.auth

import com.availelabs.availe.configuration.SharedNetworkConstants
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtUserProfileMapperTest {
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun jidClaimWithoutDomainIsNormalizedToXmppDomain() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"test@example.com","jid":"myuser"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNotNull(profile)
        assertEquals("myuser@${SharedNetworkConstants.XMPP_DOMAIN}", profile.jid.value)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun jidClaimThatLooksLikeEmailUsesLocalpartOnly() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"test@example.com","jid":"rafael@gmail.com"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNotNull(profile)
        assertEquals("rafael@${SharedNetworkConstants.XMPP_DOMAIN}", profile.jid.value)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun missingJidClaimUsesEmailLocalpartOnly() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"rafael@gmail.com"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNotNull(profile)
        assertEquals("rafael@${SharedNetworkConstants.XMPP_DOMAIN}", profile.jid.value)
    }
}