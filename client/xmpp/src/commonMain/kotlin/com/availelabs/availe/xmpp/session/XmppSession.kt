package com.availelabs.availe.xmpp.session

import com.availelabs.availe.xmpp.dto.*
import com.availelabs.availe.xmpp.model.ResourceName
import com.availelabs.availe.xmpp.model.SaslMechanism
import com.availelabs.availe.xmpp.model.StanzaIdentifier
import com.availelabs.availe.xmpp.model.XmppJid
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IXmppSession {
    val sessionState: StateFlow<SessionState>
    val incomingStanzas: SharedFlow<IXmppStreamElement>

    suspend fun connect(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String,
        host: String,
        port: Int
    )

    suspend fun disconnect()
    suspend fun sendStanza(stanza: IXmppStreamElement)
}

class XmppSession(
    private val httpClient: HttpClient
) : IXmppSession {

    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    private val mutableIncomingStanzas = MutableSharedFlow<IXmppStreamElement>()
    override val incomingStanzas: SharedFlow<IXmppStreamElement> = mutableIncomingStanzas.asSharedFlow()

    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var keepAliveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val xmlSerializer = XML {
        autoPolymorphic = true
        repairNamespaces = true
        indentString = ""
    }

    override suspend fun connect(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String,
        host: String,
        port: Int
    ) {
        if (mutableSessionState.value !is SessionState.Disconnected && mutableSessionState.value !is SessionState.Failed) {
            return
        }

        try {
            mutableSessionState.value = SessionState.Connecting

            httpClient.webSocket(
                method = HttpMethod.Get,
                host = host,
                port = port,
                path = "/ws",
                request = {
                    headers.append("Sec-WebSocket-Protocol", "xmpp")
                }
            ) {
                webSocketSession = this
                performXmppHandshake(xmppJid, authIdentity, password)
            }

            if (mutableSessionState.value !is SessionState.Failed) {
                mutableSessionState.value = SessionState.Disconnected
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Throwable) {
            val message = exception.message?.takeIf { it.isNotBlank() }
                ?: exception::class.simpleName
                ?: "Connection error"
            mutableSessionState.value = SessionState.Failed(message, exception)
        } finally {
            closeResources()
        }
    }

    override suspend fun disconnect() {
        closeResources()
        mutableSessionState.value = SessionState.Disconnected
    }

    override suspend fun sendStanza(stanza: IXmppStreamElement) {
        val session = webSocketSession ?: throw IllegalStateException("Not connected")
        val xmlString = xmlSerializer.encodeToString(IXmppStreamElement.serializer(), stanza)
        session.send(Frame.Text(xmlString))
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    private suspend fun DefaultClientWebSocketSession.performXmppHandshake(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String
    ) {
        val domain = xmppJid.value.substringAfterLast("@").substringBefore("/")

        mutableSessionState.value = SessionState.Negotiating
        val initialOpen = XmppStreamOpen(to = domain)
        send(Frame.Text(xmlSerializer.encodeToString(XmppStreamOpen.serializer(), initialOpen)))

        val features = waitForStanza<XmppStreamFeatures>()
        val mechanisms = features.mechanisms?.mechanism ?: emptyList()

        val supportsPlainMechanism = mechanisms.contains(SaslMechanism("PLAIN"))
        if (!supportsPlainMechanism) {
            val availableMechanisms = mechanisms.joinToString(", ") { mechanism -> mechanism.value }
            throw IllegalStateException("Server does not support PLAIN authentication. Available: $availableMechanisms")
        }

        val selectedMechanism = SaslMechanism("PLAIN")

        mutableSessionState.value = SessionState.Authenticating

        val authenticationString = "\u0000$authIdentity\u0000$password"

        val authenticationBase64 = Base64.encode(authenticationString.encodeToByteArray())
        val authenticationStanza = XmppSaslAuth(mechanism = selectedMechanism, content = authenticationBase64)
        send(Frame.Text(xmlSerializer.encodeToString(XmppSaslAuth.serializer(), authenticationStanza)))

        waitForStanza<XmppSaslSuccess>()

        mutableSessionState.value = SessionState.Negotiating
        val restartOpen = XmppStreamOpen(to = domain)
        send(Frame.Text(xmlSerializer.encodeToString(XmppStreamOpen.serializer(), restartOpen)))

        waitForStanza<XmppStreamFeatures>()

        mutableSessionState.value = SessionState.Binding
        val bindIdentifier = Uuid.random().toString()
        val bindStanza = XmppIqStanza(
            identifier = StanzaIdentifier(bindIdentifier),
            type = "set",
            payload = XmppBindPayload(resource = ResourceName("availe-mobile"))
        )
        send(Frame.Text(xmlSerializer.encodeToString(XmppIqStanza.serializer(), bindStanza)))

        val bindResult = waitForStanza<XmppIqStanza>()
        if (bindResult.type == "error") {
            throw IllegalStateException("Failed to bind resource")
        }

        mutableSessionState.value = SessionState.Active
        startKeepAlive()
        processIncomingLoop()
    }

    private suspend inline fun <reified T : IXmppStreamElement> DefaultClientWebSocketSession.waitForStanza(): T {
        while (isActive) {
            val frame = incoming.receive()
            if (frame is Frame.Text) {
                val text = frame.readText()
                for (elementText in splitTopLevelXmlElements(text)) {
                    detectTerminalHandshakeFailure(elementText)?.let { reason ->
                        throw IllegalStateException(reason)
                    }

                    try {
                        val element = xmlSerializer.decodeFromString(IXmppStreamElement.serializer(), elementText)
                        if (element is T) {
                            return element
                        } else if (element is XmppSaslFailure) {
                            throw IllegalStateException("SASL Authentication Failed")
                        }
                    } catch (_: Exception) {
                        if (mutableSessionState.value is SessionState.Authenticating && elementText.contains(
                                "xmpp-sasl",
                                ignoreCase = true
                            )
                        ) {
                            throw IllegalStateException("Unhandled SASL stanza: $elementText")
                        }
                    }
                }
            }
        }
        throw IllegalStateException("Connection closed before expected stanza received")
    }

    private suspend fun DefaultClientWebSocketSession.processIncomingLoop() {
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.isBlank()) continue

                    for (elementText in splitTopLevelXmlElements(text)) {
                        try {
                            val element = xmlSerializer.decodeFromString(IXmppStreamElement.serializer(), elementText)
                            mutableIncomingStanzas.emit(element)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (isActive) throw e
        }
    }

    private fun startKeepAlive() {
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(20_000)
                if (mutableSessionState.value == SessionState.Active) {
                    try {
                        webSocketSession?.send(Frame.Text(" "))
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
    }

    private suspend fun closeResources() {
        keepAliveJob?.cancelAndJoin()
        keepAliveJob = null
        try {
            webSocketSession?.close()
        } catch (_: Exception) {
        }
        webSocketSession = null
    }
}