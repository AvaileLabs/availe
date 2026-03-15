package com.availelabs.availe.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtUserProfileMapperTest {
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun jidClaimIsUsedAsIssuedByBackend() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"test@example.com","jid":"myuser@xmpp.availe.io"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNotNull(profile)
        assertEquals("myuser@xmpp.availe.io", profile.jid.value)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun jidClaimDomainIsPreserved() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"test@example.com","jid":"rafael@xmpp.availe.io"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNotNull(profile)
        assertEquals("rafael@xmpp.availe.io", profile.jid.value)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun missingJidClaimReturnsNullProfile() {
        val payloadJson =
            """{"sub":"00000000-0000-0000-0000-000000000001","email":"rafael@gmail.com"}"""
        val payloadBase64 = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        val token = "header.$payloadBase64.signature"

        val profile = JwtUserProfileMapper.decodeUserProfile(token)

        assertNull(profile)
    }
}