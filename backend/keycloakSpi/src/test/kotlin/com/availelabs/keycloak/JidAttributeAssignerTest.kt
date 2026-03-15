package com.availelabs.keycloak

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JidAttributeAssignerTest {
    private val configuration = JidEventListenerConfiguration(
        xmppDomain = XmppDomain("xmpp.availe.io")
    )

    @Test
    fun createJidValueUsesConfiguredXmppDomain() {
        val jidValue = JidAttributeAssigner.createJidValue(
            userId = "00000000-0000-0000-0000-000000000001",
            configuration = configuration
        )

        assertEquals("00000000-0000-0000-0000-000000000001@xmpp.availe.io", jidValue)
    }

    @Test
    fun createJidValueRejectsBlankUserId() {
        assertFailsWith<IllegalArgumentException> {
            JidAttributeAssigner.createJidValue(
                userId = "",
                configuration = configuration
            )
        }
    }
}