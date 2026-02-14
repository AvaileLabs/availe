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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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

    private var defaultClientWebSocketSession: DefaultClientWebSocketSession? = null
    private var keepAliveJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val xmppStreamBuffer = XmppStreamBuffer()

    private val xmlFormat = XML(
        serializersModule = SerializersModule {
            polymorphic(IIqPayload::class) {
                subclass(XmppBindPayload::class)
                subclass(XmppPingPayload::class)
            }
            polymorphic(IXmppStreamElement::class) {
                subclass(XmppStreamOpen::class)
                subclass(XmppClose::class)
                subclass(XmppStreamError::class)
                subclass(XmppStreamFeatures::class)
                subclass(XmppSaslSuccess::class)
                subclass(XmppSaslFailure::class)
                subclass(XmppSaslAuth::class)
                subclass(XmppMessageStanza::class)
                subclass(XmppPresenceStanza::class)
                subclass(XmppIqStanza::class)
            }
        }
    ) {
        repairNamespaces = true
        indentString = ""
        defaultPolicy {
            autoPolymorphic = true
            ignoreUnknownChildren()
        }
    }

    override suspend fun connect(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String,
        host: String,
        port: Int
    ) {
        println("XmppSession: Connecting to $host:$port as ${xmppJid.value}")
        if (mutableSessionState.value !is SessionState.Disconnected && mutableSessionState.value !is SessionState.Failed) {
            println("XmppSession: Connection aborted, state is ${mutableSessionState.value}")
            return
        }

        try {
            mutableSessionState.value = SessionState.Connecting
            println("XmppSession: Opening WebSocket connection...")

            httpClient.webSocket(
                method = HttpMethod.Get,
                host = host,
                port = port,
                path = "/ws",
                request = {
                    headers.append("Sec-WebSocket-Protocol", "xmpp")
                }
            ) {
                println("XmppSession: WebSocket connected. Starting handshake.")
                defaultClientWebSocketSession = this
                performXmppHandshake(xmppJid, authIdentity, password)
            }

            println("XmppSession: WebSocket session block ended cleanly.")
        } catch (cancellationException: CancellationException) {
            println("XmppSession: Connection cancelled.")
            throw cancellationException
        } catch (exception: Throwable) {
            println("XmppSession: Connection failed with exception: $exception")
            exception.printStackTrace()
            val errorMessage = exception.message?.takeIf { it.isNotBlank() }
                ?: exception::class.simpleName
                ?: "Connection error"
            mutableSessionState.value = SessionState.Failed(errorMessage, exception)
        } finally {
            closeResources()
            if (mutableSessionState.value !is SessionState.Failed) {
                mutableSessionState.value = SessionState.Disconnected
            }
        }
    }

    override suspend fun disconnect() {
        println("XmppSession: Disconnecting...")
        if (defaultClientWebSocketSession != null) {
            try {
                val xmppCloseStanza = XmppClose()
                val xmppCloseXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), xmppCloseStanza)
                defaultClientWebSocketSession?.send(Frame.Text(xmppCloseXml))
            } catch (_: Exception) {
            }
        }
        closeResources()
        mutableSessionState.value = SessionState.Disconnected
    }

    override suspend fun sendStanza(stanza: IXmppStreamElement) {
        val currentSession = defaultClientWebSocketSession ?: throw IllegalStateException("Not connected")
        val xmlString = xmlFormat.encodeToString(IXmppStreamElement.serializer(), stanza)
        println("XmppSession: Sending stanza: $xmlString")
        currentSession.send(Frame.Text(xmlString))
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    private suspend fun DefaultClientWebSocketSession.performXmppHandshake(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String
    ) {
        val domain = xmppJid.value.substringAfterLast("@").substringBefore("/")
        println("XmppSession: Handshake started for domain: $domain")

        mutableSessionState.value = SessionState.Negotiating
        val initialStreamOpen = XmppStreamOpen(to = domain)
        val initialStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), initialStreamOpen)
        println("XmppSession: Sending Stream Open: $initialStreamOpenXml")
        send(Frame.Text(initialStreamOpenXml))

        println("XmppSession: Waiting for Stream Open Response...")
        val streamOpenResponse = waitForStanza<IXmppStreamElement>()
        if (streamOpenResponse !is XmppStreamOpen) {
            println("XmppSession: Expected XmppStreamOpen but got ${streamOpenResponse::class.simpleName}")
        } else {
            println("XmppSession: Received Stream Open: $streamOpenResponse")
        }

        println("XmppSession: Waiting for Stream Features...")
        val streamFeatures = waitForStanza<XmppStreamFeatures>()
        println("XmppSession: Received Stream Features: $streamFeatures")
        val saslMechanisms = streamFeatures.mechanisms?.mechanism ?: emptyList()

        val supportsPlainMechanism = saslMechanisms.contains(SaslMechanism("PLAIN"))
        if (!supportsPlainMechanism) {
            val availableMechanisms = saslMechanisms.joinToString(", ") { mechanism -> mechanism.value }
            println("XmppSession: Error - No PLAIN mechanism. Available: $availableMechanisms")
            throw IllegalStateException("Server does not support PLAIN authentication. Available: $availableMechanisms")
        }

        val selectedMechanism = SaslMechanism("PLAIN")
        println("XmppSession: Selected mechanism: PLAIN")

        mutableSessionState.value = SessionState.Authenticating

        val authenticationString = "\u0000$authIdentity\u0000$password"

        val authenticationBase64 = Base64.encode(authenticationString.encodeToByteArray())
        val authenticationStanza = XmppSaslAuth(mechanism = selectedMechanism, content = authenticationBase64)
        val authenticationXml = xmlFormat.encodeToString(XmppSaslAuth.serializer(), authenticationStanza)
        println("XmppSession: Sending SASL Auth: $authenticationXml")
        send(Frame.Text(authenticationXml))

        println("XmppSession: Waiting for SASL Success...")
        waitForStanza<XmppSaslSuccess>()
        println("XmppSession: SASL Authentication Successful.")

        mutableSessionState.value = SessionState.Negotiating
        val restartStreamOpen = XmppStreamOpen(to = domain)
        val restartStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), restartStreamOpen)
        println("XmppSession: Restarting Stream: $restartStreamOpenXml")
        send(Frame.Text(restartStreamOpenXml))

        println("XmppSession: Waiting for Post-Auth Stream Open Response...")
        val restartStreamResponse = waitForStanza<IXmppStreamElement>()
        if (restartStreamResponse is XmppStreamOpen) {
            println("XmppSession: Received Restart Stream Open")
        }

        println("XmppSession: Waiting for Post-Auth Stream Features...")
        waitForStanza<XmppStreamFeatures>()

        mutableSessionState.value = SessionState.Binding
        val bindIdentifier = Uuid.random().toString()
        val resourceBindStanza = XmppIqStanza(
            identifier = StanzaIdentifier(bindIdentifier),
            type = "set",
            payload = XmppBindPayload(resource = ResourceName("availe-mobile"))
        )
        val resourceBindXml = xmlFormat.encodeToString(XmppIqStanza.serializer(), resourceBindStanza)
        println("XmppSession: Sending Resource Bind: $resourceBindXml")
        send(Frame.Text(resourceBindXml))

        println("XmppSession: Waiting for Bind Result...")
        val resourceBindResult = waitForStanza<XmppIqStanza>()
        println("XmppSession: Received Bind Result: $resourceBindResult")
        if (resourceBindResult.type == "error") {
            println("XmppSession: Bind failed with error.")
            throw IllegalStateException("Failed to bind resource")
        }

        println("XmppSession: Sending Initial Presence")
        val initialPresenceStanza = XmppPresenceStanza()
        val initialPresenceXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), initialPresenceStanza)
        println("XmppSession: Sending Presence Stanza: $initialPresenceXml")
        send(Frame.Text(initialPresenceXml))

        println("XmppSession: Session Active.")
        mutableSessionState.value = SessionState.Active
        startKeepAlive()
        processIncomingLoop()
    }

    private suspend inline fun <reified T : IXmppStreamElement> DefaultClientWebSocketSession.waitForStanza(): T {
        while (isActive) {
            var stanzaText = xmppStreamBuffer.extractNextStanza()

            if (stanzaText == null) {
                try {
                    val incomingFrame = incoming.receive()
                    if (incomingFrame is Frame.Text) {
                        val frameText = incomingFrame.readText()
                        println("XmppSession: Received Frame: $frameText")
                        xmppStreamBuffer.append(frameText)
                        stanzaText = xmppStreamBuffer.extractNextStanza()
                    }
                } catch (exception: Exception) {
                    println("XmppSession: Error receiving frame: $exception")
                    throw IllegalStateException("Connection closed while waiting for stanza", exception)
                }
            }

            if (stanzaText != null) {
                println("XmppSession: Processing Stanza Text: $stanzaText")
                val parsableStanza =
                    if ((stanzaText.startsWith("<stream:stream") || stanzaText.startsWith("<open")) && !stanzaText.trimEnd()
                            .endsWith("/>")
                    ) {
                        val endIndex = stanzaText.lastIndexOf(">")
                        if (endIndex != -1) {
                            stanzaText.substring(0, endIndex) + "/>"
                        } else {
                            stanzaText
                        }
                    } else {
                        stanzaText
                    }

                try {
                    val decodedElement = xmlFormat.decodeFromString(IXmppStreamElement.serializer(), parsableStanza)
                    println("XmppSession: Decoded Element: ${decodedElement::class.simpleName}")

                    if (decodedElement is T) {
                        return decodedElement
                    } else if (decodedElement is XmppSaslFailure) {
                        println("XmppSession: SASL Failure received: ${decodedElement.notAuthorized}")
                        throw IllegalStateException("SASL Authentication Failed: ${decodedElement.notAuthorized}")
                    } else if (decodedElement is XmppStreamError) {
                        println("XmppSession: Stream Error received: ${decodedElement.text}")
                        throw IllegalStateException("XMPP Stream Error: ${decodedElement.text}")
                    } else if (decodedElement is XmppClose) {
                        println("XmppSession: Server closed the stream.")
                        throw IllegalStateException("Server closed the stream")
                    } else if (decodedElement is XmppStreamOpen) {
                        if (decodedElement is T) return decodedElement
                    } else {
                        println("XmppSession: Ignored unexpected element type during handshake: ${decodedElement::class.simpleName}")
                    }
                } catch (exception: Exception) {
                    println("XmppSession: Parsing Error: $exception")
                    exception.printStackTrace()
                    if (mutableSessionState.value is SessionState.Authenticating || mutableSessionState.value is SessionState.Binding) {
                        throw IllegalStateException("Parsing error during handshake: ${exception.message}")
                    }
                }
            }
        }
        throw IllegalStateException("Connection closed before expected stanza received")
    }

    private suspend fun DefaultClientWebSocketSession.processIncomingLoop() {
        try {
            for (incomingFrame in incoming) {
                if (incomingFrame is Frame.Text) {
                    val frameText = incomingFrame.readText()
                    println("XmppSession: Loop Received Frame: $frameText")
                    xmppStreamBuffer.append(frameText)

                    while (true) {
                        val stanzaText = xmppStreamBuffer.extractNextStanza() ?: break
                        println("XmppSession: Loop Processing Stanza: $stanzaText")
                        try {
                            val decodedElement = xmlFormat.decodeFromString(IXmppStreamElement.serializer(), stanzaText)
                            println("XmppSession: Loop Emitting Element: ${decodedElement::class.simpleName}")

                            when (decodedElement) {
                                is XmppClose -> {
                                    println("XmppSession: Server sent <close/>. Stopping loop.")
                                    return
                                }

                                is XmppStreamError -> {
                                    println("XmppSession: Server sent stream error: ${decodedElement.text}")
                                    throw IllegalStateException("Stream Error: ${decodedElement.text}")
                                }

                                else -> {
                                    mutableIncomingStanzas.emit(decodedElement)
                                }
                            }
                        } catch (exception: Exception) {
                            println("XmppSession: Loop Parsing Error: $exception")
                            exception.printStackTrace()
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            println("XmppSession: Loop Exception: $exception")
            if (isActive) throw exception
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun startKeepAlive() {
        keepAliveJob = coroutineScope.launch {
            while (isActive) {
                delay(30_000)
                if (mutableSessionState.value == SessionState.Active) {
                    try {
                        println("XmppSession: Sending XMPP Ping IQ")
                        val pingStanza = XmppIqStanza(
                            identifier = StanzaIdentifier(Uuid.random().toString()),
                            type = "get",
                            payload = XmppPingPayload()
                        )
                        val xmlString = xmlFormat.encodeToString(IXmppStreamElement.serializer(), pingStanza)
                        defaultClientWebSocketSession?.send(Frame.Text(xmlString))
                    } catch (exception: Exception) {
                        println("XmppSession: KeepAlive failed: $exception")
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
            defaultClientWebSocketSession?.close()
        } catch (exception: Exception) {
            println("XmppSession: Error closing socket: $exception")
        }
        defaultClientWebSocketSession = null
    }
}