package com.availelabs.availe.xmpp.session

import com.availelabs.availe.xmpp.model.XmppJid
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

class XmppSessionEjabberdIntegrationTest {
    @Test
    fun connectsToEjabberdOrSurfacesFailureReason() = runBlocking {
        val enabled = System.getenv("AVAILE_XMPP_INTEGRATION_TESTS")?.lowercase() == "true"
        if (!enabled) {
            println("[DEBUG_LOG] Skipping XMPP integration test (AVAILE_XMPP_INTEGRATION_TESTS != true)")
            return@runBlocking
        }

        val host = System.getenv("AVAILE_XMPP_HOST") ?: "localhost"
        val port = System.getenv("AVAILE_XMPP_PORT")?.toIntOrNull() ?: 5280
        val jid = System.getenv("AVAILE_XMPP_JID")
        val password = System.getenv("AVAILE_XMPP_PASSWORD")

        if (jid.isNullOrBlank() || password.isNullOrBlank()) {
            println("[DEBUG_LOG] Skipping XMPP integration test (AVAILE_XMPP_JID or AVAILE_XMPP_PASSWORD not set)")
            return@runBlocking
        }

        val httpClient = HttpClient(CIO) {
            install(WebSockets)
        }

        val session = XmppSession(httpClient)

        val connectJob = launch(Dispatchers.IO) {
            session.connect(
                xmppJid = XmppJid(jid),
                password = password,
                host = host,
                port = port
            )
        }

        val finalState = try {
            withTimeout(20_000) {
                session.sessionState
                    .filter { it is SessionState.Active || it is SessionState.Failed }
                    .first()
            }
        } catch (e: TimeoutCancellationException) {
            println("[DEBUG_LOG] XMPP integration timed out. lastState=${session.sessionState.value}")
            session.disconnect()
            connectJob.cancelAndJoin()
            httpClient.close()
            throw e
        }

        println("[DEBUG_LOG] XMPP integration finalState=$finalState")
        if (finalState is SessionState.Failed) {
            println("[DEBUG_LOG] XMPP integration exceptionType=${finalState.exception?.javaClass?.name}")
            finalState.exception?.printStackTrace()
        }

        session.disconnect()
        connectJob.cancelAndJoin()
        httpClient.close()

        assertTrue(
            finalState is SessionState.Active,
            "XMPP session did not become Active. finalState=$finalState"
        )
    }
}