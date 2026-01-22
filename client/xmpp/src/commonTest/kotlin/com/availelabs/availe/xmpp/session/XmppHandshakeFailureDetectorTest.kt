package com.availelabs.availe.xmpp.session

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XmppHandshakeFailureDetectorTest {
    @Test
    fun saslFailureNotAuthorizedIncludesCondition() {
        val raw = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><not-authorized/></failure>"
        val reason = detectTerminalHandshakeFailure(raw)

        assertNotNull(reason)
        assertTrue(reason.contains("SASL Authentication Failed"))
        assertTrue(reason.contains("not-authorized", ignoreCase = true))
    }

    @Test
    fun saslChallengeIsDetected() {
        val raw = "<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>abc</challenge>"
        val reason = detectTerminalHandshakeFailure(raw)

        assertNotNull(reason)
        assertTrue(reason.contains("challenge", ignoreCase = true))
    }

    @Test
    fun streamErrorIsDetected() {
        val raw =
            "<stream:error xmlns:stream='http://etherx.jabber.org/streams'><not-well-formed xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error>"
        val reason = detectTerminalHandshakeFailure(raw)

        assertNotNull(reason)
        assertTrue(reason.contains("Stream error"))
    }
}