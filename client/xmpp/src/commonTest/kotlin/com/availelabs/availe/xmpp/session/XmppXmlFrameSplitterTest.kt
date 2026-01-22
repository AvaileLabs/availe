package com.availelabs.availe.xmpp.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmppXmlFrameSplitterTest {
    @Test
    fun splitsMultipleTopLevelElementsInOneFrame() {
        val raw = "<open xmlns='urn:ietf:params:xml:ns:xmpp-framing' to='localhost' version='1.0'/>" +
                "<stream:features xmlns:stream='http://etherx.jabber.org/streams'/>"

        val parts = splitTopLevelXmlElements(raw)

        assertEquals(2, parts.size)
        assertTrue(parts[0].startsWith("<open"))
        assertTrue(parts[1].startsWith("<stream:features"))
    }

    @Test
    fun keepsSingleElementAsSinglePart() {
        val raw = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>"

        val parts = splitTopLevelXmlElements(raw)

        assertEquals(listOf(raw), parts)
    }

    @Test
    fun handlesNestedElements() {
        val raw =
            "<stream:features xmlns:stream='http://etherx.jabber.org/streams'><mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>PLAIN</mechanism></mechanisms></stream:features>" +
                    "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><not-authorized/></failure>"

        val parts = splitTopLevelXmlElements(raw)

        assertEquals(2, parts.size)
        assertTrue(parts[0].contains("<mechanisms"))
        assertTrue(parts[1].startsWith("<failure"))
    }
}