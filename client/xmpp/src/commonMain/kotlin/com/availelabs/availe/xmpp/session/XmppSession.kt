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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.collections.ArrayDeque
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
    private val connectionMutex = Mutex()

    private data class SmState(
        var enabled: Boolean = false,
        var smId: String? = null,
        var inH: Long = 0L,
        var outSeq: Long = 0L,
        var lastAckedByServer: Long = 0L,
        val unacked: ArrayDeque<String> = ArrayDeque()
    )

    private val smState = SmState()

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
                subclass(XmppSmEnable::class)
                subclass(XmppSmEnabled::class)
                subclass(XmppSmResume::class)
                subclass(XmppSmResumed::class)
                subclass(XmppSmFailed::class)
                subclass(XmppSmRequest::class)
                subclass(XmppSmAck::class)
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
        connectionMutex.withLock {
            println("XmppSession: Attempting connection to host: $host, port: $port for JID: ${xmppJid.value}")

            if (mutableSessionState.value !is SessionState.Disconnected && mutableSessionState.value !is SessionState.Failed) {
                println("XmppSession: Found active/connecting session (${mutableSessionState.value}). Forcing disconnect before reconnecting.")
                performInternalDisconnect()
            }

            try {
                mutableSessionState.value = SessionState.Connecting
                println("XmppSession: Initiating WebSocket connection request...")

                httpClient.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws",
                    request = {
                        headers.append("Sec-WebSocket-Protocol", "xmpp")
                    }
                ) {
                    println("XmppSession: WebSocket connection established successfully. Starting XMPP handshake.")
                    defaultClientWebSocketSession = this
                    performXmppHandshake(xmppJid, authIdentity, password)
                }

                println("XmppSession: WebSocket session block has completed execution.")
            } catch (cancellationException: CancellationException) {
                println("XmppSession: Connection was cancelled by the coroutine scope.")
                throw cancellationException
            } catch (exception: Throwable) {
                println("XmppSession: Connection encountered a terminal exception: ${exception.message}")
                exception.printStackTrace()
                val errorMessage = exception.message?.takeIf { it.isNotBlank() }
                    ?: exception::class.simpleName
                    ?: "Unknown connection error"
                mutableSessionState.value = SessionState.Failed(errorMessage, exception)
            } finally {
                println("XmppSession: Cleaning up session resources. Previous state was: ${mutableSessionState.value}")
                performInternalDisconnect()
                if (mutableSessionState.value !is SessionState.Failed) {
                    println("XmppSession: Transitioning state to Disconnected.")
                    mutableSessionState.value = SessionState.Disconnected
                } else {
                    println("XmppSession: Retaining Failed state for error reporting.")
                }
            }
        }
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {
            println("XmppSession: Explicit disconnect requested.")
            if (defaultClientWebSocketSession != null) {
                try {
                    val xmppCloseStanza = XmppClose()
                    val xmppCloseXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), xmppCloseStanza)
                    println("XmppSession: Sending stream close stanza.")
                    defaultClientWebSocketSession?.send(Frame.Text(xmppCloseXml))
                } catch (exception: Exception) {
                    println("XmppSession: Failed to send close stanza: ${exception.message}")
                }
            }
            performInternalDisconnect()
            smState.enabled = false
            smState.smId = null
            smState.unacked.clear()
            smState.inH = 0
            smState.outSeq = 0

            mutableSessionState.value = SessionState.Disconnected
        }
    }

    private suspend fun performInternalDisconnect() {
        keepAliveJob?.cancelAndJoin()
        keepAliveJob = null
        try {
            defaultClientWebSocketSession?.close()
        } catch (e: Exception) {
        }
        defaultClientWebSocketSession = null
    }

    override suspend fun sendStanza(stanza: IXmppStreamElement) {
        val currentSession = defaultClientWebSocketSession
            ?: run {
                println("XmppSession: Cannot send stanza. No active WebSocket session exists.")
                throw IllegalStateException("Not connected")
            }

        val xmlString = xmlFormat.encodeToString(IXmppStreamElement.serializer(), stanza)
        println("XmppSession: Transmitting stanza: $xmlString")

        if (smState.enabled && isCountableStanza(stanza)) {
            smState.outSeq++
            smState.unacked.addLast(xmlString)
            println("XmppSession: [SM] Incremented outSeq to ${smState.outSeq}, queued stanza.")
        }

        currentSession.send(Frame.Text(xmlString))
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    private suspend fun DefaultClientWebSocketSession.performXmppHandshake(
        xmppJid: XmppJid,
        authIdentity: String,
        password: String
    ) {
        val domain = xmppJid.value.substringAfterLast("@").substringBefore("/")
        println("XmppSession: Beginning handshake for domain: $domain")

        mutableSessionState.value = SessionState.Negotiating
        val initialStreamOpen = XmppStreamOpen(to = domain)
        val initialStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), initialStreamOpen)
        println("XmppSession: Sending initial stream open: $initialStreamOpenXml")
        send(Frame.Text(initialStreamOpenXml))

        println("XmppSession: Awaiting initial stream open response from server...")
        val streamOpenResponse = waitForStanza<IXmppStreamElement>()
        if (streamOpenResponse !is XmppStreamOpen) {
            println("XmppSession: Unexpected response received instead of stream open: ${streamOpenResponse::class.simpleName}")
        } else {
            println("XmppSession: Initial stream open response received: $streamOpenResponse")
        }

        println("XmppSession: Awaiting server stream features...")
        val streamFeatures = waitForStanza<XmppStreamFeatures>()
        println("XmppSession: Received server stream features: $streamFeatures")
        val saslMechanisms = streamFeatures.mechanisms?.mechanism ?: emptyList()

        val supportsPlainMechanism = saslMechanisms.contains(SaslMechanism("PLAIN"))
        if (!supportsPlainMechanism) {
            throw IllegalStateException("Server does not support PLAIN authentication.")
        }

        val selectedMechanism = SaslMechanism("PLAIN")
        println("XmppSession: Selecting PLAIN authentication mechanism.")

        mutableSessionState.value = SessionState.Authenticating

        val authenticationString = "\u0000$authIdentity\u0000$password"
        val authenticationBase64 = Base64.encode(authenticationString.encodeToByteArray())
        val authenticationStanza = XmppSaslAuth(mechanism = selectedMechanism, content = authenticationBase64)
        val authenticationXml = xmlFormat.encodeToString(XmppSaslAuth.serializer(), authenticationStanza)
        println("XmppSession: Sending SASL authentication request.")
        send(Frame.Text(authenticationXml))

        println("XmppSession: Awaiting SASL authentication result...")
        waitForStanza<XmppSaslSuccess>()
        println("XmppSession: SASL authentication was successful.")

        mutableSessionState.value = SessionState.Negotiating
        val restartStreamOpen = XmppStreamOpen(to = domain)
        val restartStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), restartStreamOpen)
        println("XmppSession: Sending stream restart open: $restartStreamOpenXml")
        send(Frame.Text(restartStreamOpenXml))

        println("XmppSession: Awaiting post-authentication stream open response...")
        waitForStanza<XmppStreamOpen>()
        println("XmppSession: Received stream restart confirmation.")

        println("XmppSession: Awaiting post-authentication stream features...")
        val postAuthFeatures = waitForStanza<XmppStreamFeatures>()

        var isResumed = false
        if (postAuthFeatures.sm != null) {
            if (smState.smId != null) {
                println("XmppSession: [SM] Attempting resumption with previd=${smState.smId}, h=${smState.inH}")
                val resumeStanza = XmppSmResume(previd = smState.smId!!, h = smState.inH)
                val resumeXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), resumeStanza)
                send(Frame.Text(resumeXml))

                val resumeResponse = waitForStanza<IXmppStreamElement>()
                if (resumeResponse is XmppSmResumed) {
                    println("XmppSession: [SM] Session Resumed! Server h=${resumeResponse.h}")
                    isResumed = true
                    processAck(resumeResponse.h)
                    if (smState.unacked.isNotEmpty()) {
                        println("XmppSession: [SM] Replaying ${smState.unacked.size} unacked stanzas.")
                        for (xml in smState.unacked) {
                            send(Frame.Text(xml))
                        }
                    }
                } else {
                    println("XmppSession: [SM] Resumption failed (Got ${resumeResponse::class.simpleName}). Clearing SM state.")
                    smState.smId = null
                    smState.unacked.clear()
                    smState.inH = 0
                    smState.outSeq = 0
                    smState.enabled = false
                }
            }

            if (!isResumed) {
                println("XmppSession: [SM] Enabling Stream Management.")
                val enableStanza = XmppSmEnable(resume = "true")
                val enableXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), enableStanza)
                send(Frame.Text(enableXml))

                val enableResponse = waitForStanza<IXmppStreamElement>()
                if (enableResponse is XmppSmEnabled) {
                    println("XmppSession: [SM] Stream Management Enabled. ID=${enableResponse.id}")
                    smState.enabled = true
                    smState.smId = enableResponse.id
                    smState.inH = 0
                    smState.outSeq = 0
                    smState.unacked.clear()
                } else {
                    println("XmppSession: [SM] Failed to enable Stream Management.")
                }
            }
        }

        if (!isResumed) {
            mutableSessionState.value = SessionState.Binding
            val bindIdentifier = Uuid.random().toString()
            val randomResource = "availe-mobile-${Uuid.random()}"
            val resourceBindStanza = XmppIqStanza(
                identifier = StanzaIdentifier(bindIdentifier),
                type = "set",
                payload = XmppBindPayload(resource = ResourceName(randomResource))
            )
            val resourceBindXml = xmlFormat.encodeToString(XmppIqStanza.serializer(), resourceBindStanza)
            println("XmppSession: Sending resource bind request: $resourceBindXml")
            send(Frame.Text(resourceBindXml))

            println("XmppSession: Awaiting resource bind result...")
            val resourceBindResult = waitForStanza<XmppIqStanza>()
            println("XmppSession: Received resource bind result: $resourceBindResult")
            if (resourceBindResult.type == "error") {
                println("XmppSession: Resource binding failed with a server error.")
                throw IllegalStateException("Failed to bind resource")
            }

            println("XmppSession: Sending initial presence stanza.")
            val initialPresenceStanza = XmppPresenceStanza()
            val initialPresenceXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), initialPresenceStanza)
            println("XmppSession: Transmitting presence: $initialPresenceXml")
            send(Frame.Text(initialPresenceXml))
        }

        println("XmppSession: XMPP Session is now fully active.")
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
                        xmppStreamBuffer.append(frameText)
                        stanzaText = xmppStreamBuffer.extractNextStanza()
                    }
                } catch (exception: Exception) {
                    println("XmppSession: Error while receiving frame from socket: ${exception.message}")
                    throw IllegalStateException("Connection closed while waiting for stanza", exception)
                }
            }

            if (stanzaText != null) {
                println("XmppSession: Handshake received: $stanzaText")
                val parsableStanza = prepareStanzaForParsing(stanzaText)

                try {
                    val decodedElement = xmlFormat.decodeFromString(IXmppStreamElement.serializer(), parsableStanza)

                    if (decodedElement is T) {
                        return decodedElement
                    } else if (decodedElement is XmppSaslFailure) {
                        throw IllegalStateException("SASL Authentication Failed: ${decodedElement.notAuthorized}")
                    } else if (decodedElement is XmppStreamError) {
                        throw IllegalStateException("XMPP Stream Error: ${decodedElement.text}")
                    } else if (decodedElement is XmppClose) {
                        throw IllegalStateException("Server closed the stream")
                    } else if (decodedElement is XmppSmFailed) {
                        if (decodedElement is T) return decodedElement
                    }
                } catch (exception: Exception) {
                    println("XmppSession: Failed to parse stanza during handshake: ${exception.message}")
                    exception.printStackTrace()
                    throw exception
                }
            }
        }
        throw IllegalStateException("Connection closed before expected stanza received")
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun DefaultClientWebSocketSession.processIncomingLoop() {
        println("XmppSession: Entering main incoming stanza processing loop.")
        try {
            for (incomingFrame in incoming) {
                if (incomingFrame is Frame.Text) {
                    val frameText = incomingFrame.readText()
                    xmppStreamBuffer.append(frameText)

                    while (true) {
                        val stanzaText = xmppStreamBuffer.extractNextStanza() ?: break
                        try {
                            val decodedElement = xmlFormat.decodeFromString(IXmppStreamElement.serializer(), stanzaText)

                            when (decodedElement) {
                                is XmppSmRequest -> {
                                    println("XmppSession: [SM] Server requested ACK. Sending h=${smState.inH}")
                                    val ackStanza = XmppSmAck(h = smState.inH)
                                    val ackXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), ackStanza)
                                    send(Frame.Text(ackXml))
                                }
                                is XmppSmAck -> {
                                    println("XmppSession: [SM] Server ACK'd h=${decodedElement.h}")
                                    processAck(decodedElement.h)
                                }
                                is XmppClose -> {
                                    println("XmppSession: Received server-initiated stream close.")
                                    return
                                }
                                is XmppStreamError -> {
                                    println("XmppSession: Received server-initiated stream error: ${decodedElement.text}")
                                    throw IllegalStateException("Stream Error: ${decodedElement.text}")
                                }
                                is XmppIqStanza -> {
                                    val payload = decodedElement.payload
                                    if (payload is XmppPingPayload && decodedElement.type == "get") {
                                        println("XmppSession: Received Ping from server. Replying Pong.")
                                        val pongStanza = XmppIqStanza(
                                            identifier = decodedElement.identifier,
                                            type = "result",
                                            recipientJid = decodedElement.senderJid,
                                            senderJid = null
                                        )
                                        val pongXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), pongStanza)
                                        send(Frame.Text(pongXml))
                                    } else {
                                        if (smState.enabled && isCountableStanza(decodedElement)) {
                                            smState.inH++
                                        }
                                        mutableIncomingStanzas.emit(decodedElement)
                                    }
                                }
                                else -> {
                                    if (smState.enabled && isCountableStanza(decodedElement)) {
                                        smState.inH++
                                    }
                                    mutableIncomingStanzas.emit(decodedElement)
                                }
                            }
                        } catch (exception: Exception) {
                            println("XmppSession: Stanza decoding failed in loop: ${exception.message}")
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            println("XmppSession: Incoming loop encountered an exception: ${exception.message}")
            if (isActive) throw exception
        } finally {
            println("XmppSession: Exiting main incoming stanza processing loop.")
        }
    }

    private fun processAck(h: Long) {
        val count = h - smState.lastAckedByServer
        if (count > 0) {
            for (i in 0 until count) {
                if (smState.unacked.isNotEmpty()) {
                    smState.unacked.removeFirst()
                }
            }
            smState.lastAckedByServer = h
            println("XmppSession: [SM] Removed $count acknowledged stanzas from queue. Remaining: ${smState.unacked.size}")
        }
    }

    private fun isCountableStanza(stanza: IXmppStreamElement): Boolean {
        return stanza is XmppMessageStanza || stanza is XmppPresenceStanza || stanza is XmppIqStanza
    }

    private fun prepareStanzaForParsing(stanzaText: String): String {
        if ((stanzaText.startsWith("<stream:stream") || stanzaText.startsWith("<open")) && !stanzaText.trimEnd()
                .endsWith("/>")
        ) {
            val endIndex = stanzaText.lastIndexOf(">")
            if (endIndex != -1) {
                return stanzaText.substring(0, endIndex) + "/>"
            }
        }
        return stanzaText
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun startKeepAlive() {
        println("XmppSession: Starting background keep-alive task.")
        keepAliveJob = coroutineScope.launch {
            while (isActive) {
                delay(30_000)
                if (mutableSessionState.value == SessionState.Active) {
                    try {
                        val pingStanza = XmppIqStanza(
                            identifier = StanzaIdentifier(Uuid.random().toString()),
                            type = "get",
                            payload = XmppPingPayload()
                        )
                        sendStanza(pingStanza)
                    } catch (exception: Exception) {
                        println("XmppSession: Keep-alive ping failed to send: ${exception.message}")
                        break
                    }
                }
            }
        }
    }
}