package com.availelabs.availe.xmpp.session

import com.availelabs.availe.xmpp.dto.XmppStreamOpen
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertTrue

class EjabberdWebSocketProbeIntegrationTest {
    @Test
    fun printsServerFramesAndCloseReason() = runBlocking {
        val enabled = System.getenv("AVAILE_XMPP_INTEGRATION_TESTS")?.lowercase() == "true"
        if (!enabled) {
            println("[DEBUG_LOG] Skipping Ejabberd WS probe (AVAILE_XMPP_INTEGRATION_TESTS != true)")
            return@runBlocking
        }

        val host = System.getenv("AVAILE_XMPP_HOST") ?: "localhost"
        val port = System.getenv("AVAILE_XMPP_PORT")?.toIntOrNull() ?: 5280
        val domain = System.getenv("AVAILE_XMPP_DOMAIN") ?: "localhost"

        val httpClient = HttpClient(CIO) {
            install(WebSockets)
        }

        val xmlSerializer = XML {
            autoPolymorphic = true
            repairNamespaces = true
            indentString = ""
        }

        val receivedFrames = mutableListOf<String>()
        var serverCloseReason: CloseReason? = null

        try {
            withTimeout(10_000) {
                httpClient.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws",
                    request = {
                        headers.append("Sec-WebSocket-Protocol", "xmpp")
                    }
                ) {
                    val open = XmppStreamOpen(to = domain)
                    val openXml = xmlSerializer.encodeToString(XmppStreamOpen.serializer(), open)
                    send(Frame.Text(openXml))

                    repeat(10) {
                        val next = withTimeoutOrNull(1_000) { incoming.receive() } ?: return@repeat
                        if (next is Frame.Text) {
                            receivedFrames += next.readText()
                        }
                    }

                    serverCloseReason = withTimeoutOrNull(500) { closeReason.await() }
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("[DEBUG_LOG] Ejabberd WS probe timed out")
            throw e
        } finally {
            httpClient.close()
        }

        println("[DEBUG_LOG] Ejabberd WS probe receivedFramesCount=${receivedFrames.size}")
        receivedFrames.forEachIndexed { index, text ->
            println("[DEBUG_LOG] Ejabberd WS probe frame[$index]=$text")
        }
        println("[DEBUG_LOG] Ejabberd WS probe closeReason=$serverCloseReason")

        assertTrue(
            receivedFrames.isNotEmpty() || serverCloseReason != null,
            "No frames received and no close reason observed"
        )
    }
}