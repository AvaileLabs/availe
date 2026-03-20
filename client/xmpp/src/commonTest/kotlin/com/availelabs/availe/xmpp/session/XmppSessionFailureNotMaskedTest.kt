package com.availelabs.availe.xmpp.session

import com.availelabs.availe.xmpp.model.XmppJid
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class XmppSessionFailureNotMaskedTest {
    @Test
    fun connectFailureDoesNotBecomeDisconnected() = runTest {
        val httpClient = HttpClient(
            MockEngine { throw IllegalStateException("boom") }
        ) {
            install(WebSockets)
        }

        val session = XmppSession(httpClient)

        session.connect(
            xmppJid = XmppJid("user@localhost"),
            authIdentity = "user",
            password = "password",
            host = "localhost",
            port = 5280
        )

        assertTrue(session.sessionState.value is SessionState.Failed)
    }
}