package com.availelabs.availe.xmpp.session

import com.availelabs.availe.xmpp.model.XmppJid
import io.ktor.client.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class XmppSessionFailureNotMaskedTest {
    @Test
    fun connectFailureDoesNotBecomeDisconnected() = runBlocking {
        val httpClient = HttpClient(
            MockEngine { throw IllegalStateException("boom") }
        ) {
            install(WebSockets)
        }

        val session = XmppSession(httpClient)

        session.connect(
            xmppJid = XmppJid("user@localhost"),
            password = "password",
            host = "localhost",
            port = 5280
        )

        assertTrue(session.sessionState.value is SessionState.Failed)
    }
}