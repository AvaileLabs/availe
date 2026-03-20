package com.availelabs.availe.xmpp.session

import com.availelabs.availe.logging.LogCategory
import com.availelabs.availe.logging.logger
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

    private val classLogger by logger(LogCategory.XMPP)

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
            classLogger.i { "Attempting connection to host: $host, port: $port for JID: ${xmppJid.value}" }

            if (mutableSessionState.value !is SessionState.Disconnected && mutableSessionState.value !is SessionState.Failed) {
                classLogger.w { "Found active/connecting session (${mutableSessionState.value}). Forcing disconnect before reconnecting." }
                performInternalDisconnect()
            }

            try {
                mutableSessionState.value = SessionState.Connecting
                classLogger.d { "Initiating WebSocket connection request..." }

                httpClient.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws",
                    request = {
                        headers.append("Sec-WebSocket-Protocol", "xmpp")
                    }
                ) {
                    classLogger.i { "WebSocket connection established successfully. Starting XMPP handshake." }
                    defaultClientWebSocketSession = this
                    performXmppHandshake(xmppJid, authIdentity, password)
                }

                classLogger.d { "WebSocket session block has completed execution." }
            } catch (cancellationException: CancellationException) {
                classLogger.w(cancellationException) { "Connection was cancelled by the coroutine scope." }
                throw cancellationException
            } catch (exception: Throwable) {
                classLogger.e(exception) { "Connection encountered a terminal exception." }
                val errorMessage = exception.message?.takeIf { it.isNotBlank() }
                    ?: exception::class.simpleName
                    ?: "Unknown connection error"
                mutableSessionState.value = SessionState.Failed(errorMessage, exception)
            } finally {
                classLogger.d { "Cleaning up session resources. Previous state was: ${mutableSessionState.value}" }
                performInternalDisconnect()
                if (mutableSessionState.value !is SessionState.Failed) {
                    classLogger.d { "Transitioning state to Disconnected." }
                    mutableSessionState.value = SessionState.Disconnected
                } else {
                    classLogger.d { "Retaining Failed state for error reporting." }
                }
            }
        }
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {
            classLogger.i { "Explicit disconnect requested." }
            if (defaultClientWebSocketSession != null) {
                try {
                    val xmppCloseStanza = XmppClose()
                    val xmppCloseXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), xmppCloseStanza)
                    classLogger.d { "Sending stream close stanza." }
                    defaultClientWebSocketSession?.send(Frame.Text(xmppCloseXml))
                } catch (exception: Exception) {
                    classLogger.w(exception) { "Failed to send close stanza." }
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
        } catch (exception: Exception) {
        }
        defaultClientWebSocketSession = null
    }

    override suspend fun sendStanza(stanza: IXmppStreamElement) {
        val currentSession = defaultClientWebSocketSession
            ?: run {
                classLogger.w { "Cannot send stanza. No active WebSocket session exists." }
                throw IllegalStateException("Not connected")
            }

        val xmlString = xmlFormat.encodeToString(IXmppStreamElement.serializer(), stanza)
        classLogger.v { "Transmitting stanza: $xmlString" }

        if (smState.enabled && isCountableStanza(stanza)) {
            smState.outSeq++
            smState.unacked.addLast(xmlString)
            classLogger.v { "[SM] Incremented outSeq to ${smState.outSeq}, queued stanza." }
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
        classLogger.i { "Beginning handshake for domain: $domain" }

        mutableSessionState.value = SessionState.Negotiating
        val initialStreamOpen = XmppStreamOpen(to = domain)
        val initialStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), initialStreamOpen)
        classLogger.d { "Sending initial stream open: $initialStreamOpenXml" }
        send(Frame.Text(initialStreamOpenXml))

        classLogger.d { "Awaiting initial stream open response from server..." }
        val streamOpenResponse = waitForStanza<IXmppStreamElement>()
        if (streamOpenResponse !is XmppStreamOpen) {
            classLogger.w { "Unexpected response received instead of stream open: ${streamOpenResponse::class.simpleName}" }
        } else {
            classLogger.d { "Initial stream open response received: $streamOpenResponse" }
        }

        classLogger.d { "Awaiting server stream features..." }
        val streamFeatures = waitForStanza<XmppStreamFeatures>()
        classLogger.d { "Received server stream features: $streamFeatures" }
        val saslMechanisms = streamFeatures.mechanisms?.mechanism ?: emptyList()

        val supportsPlainMechanism = saslMechanisms.contains(SaslMechanism("PLAIN"))
        if (!supportsPlainMechanism) {
            throw IllegalStateException("Server does not support PLAIN authentication.")
        }

        val selectedMechanism = SaslMechanism("PLAIN")
        classLogger.i { "Selecting PLAIN authentication mechanism." }

        mutableSessionState.value = SessionState.Authenticating

        val authenticationString = "\u0000$authIdentity\u0000$password"
        val authenticationBase64 = Base64.encode(authenticationString.encodeToByteArray())
        val authenticationStanza = XmppSaslAuth(mechanism = selectedMechanism, content = authenticationBase64)
        val authenticationXml = xmlFormat.encodeToString(XmppSaslAuth.serializer(), authenticationStanza)
        classLogger.d { "Sending SASL authentication request." }
        send(Frame.Text(authenticationXml))

        classLogger.d { "Awaiting SASL authentication result..." }
        waitForStanza<XmppSaslSuccess>()
        classLogger.i { "SASL authentication was successful." }

        mutableSessionState.value = SessionState.Negotiating
        val restartStreamOpen = XmppStreamOpen(to = domain)
        val restartStreamOpenXml = xmlFormat.encodeToString(XmppStreamOpen.serializer(), restartStreamOpen)
        classLogger.d { "Sending stream restart open: $restartStreamOpenXml" }
        send(Frame.Text(restartStreamOpenXml))

        classLogger.d { "Awaiting post-authentication stream open response..." }
        waitForStanza<XmppStreamOpen>()
        classLogger.d { "Received stream restart confirmation." }

        classLogger.d { "Awaiting post-authentication stream features..." }
        val postAuthFeatures = waitForStanza<XmppStreamFeatures>()

        var isResumed = false
        if (postAuthFeatures.sm != null) {
            if (smState.smId != null) {
                classLogger.i { "[SM] Attempting resumption with previd=${smState.smId}, h=${smState.inH}" }
                val resumeStanza = XmppSmResume(previd = smState.smId!!, h = smState.inH)
                val resumeXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), resumeStanza)
                send(Frame.Text(resumeXml))

                val resumeResponse = waitForStanza<IXmppStreamElement>()
                if (resumeResponse is XmppSmResumed) {
                    classLogger.i { "[SM] Session Resumed! Server h=${resumeResponse.h}" }
                    isResumed = true
                    processAck(resumeResponse.h)
                    if (smState.unacked.isNotEmpty()) {
                        classLogger.i { "[SM] Replaying ${smState.unacked.size} unacked stanzas." }
                        for (xml in smState.unacked) {
                            send(Frame.Text(xml))
                        }
                    }
                } else {
                    classLogger.w { "[SM] Resumption failed (Got ${resumeResponse::class.simpleName}). Clearing SM state." }
                    smState.smId = null
                    smState.unacked.clear()
                    smState.inH = 0
                    smState.outSeq = 0
                    smState.enabled = false
                }
            }

            if (!isResumed) {
                classLogger.i { "[SM] Enabling Stream Management." }
                val enableStanza = XmppSmEnable(resume = "true")
                val enableXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), enableStanza)
                send(Frame.Text(enableXml))

                val enableResponse = waitForStanza<IXmppStreamElement>()
                if (enableResponse is XmppSmEnabled) {
                    classLogger.i { "[SM] Stream Management Enabled. ID=${enableResponse.id}" }
                    smState.enabled = true
                    smState.smId = enableResponse.id
                    smState.inH = 0
                    smState.outSeq = 0
                    smState.unacked.clear()
                } else {
                    classLogger.w { "[SM] Failed to enable Stream Management." }
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
            classLogger.d { "Sending resource bind request: $resourceBindXml" }
            send(Frame.Text(resourceBindXml))

            classLogger.d { "Awaiting resource bind result..." }
            val resourceBindResult = waitForStanza<XmppIqStanza>()
            classLogger.d { "Received resource bind result: $resourceBindResult" }
            if (resourceBindResult.type == "error") {
                classLogger.e { "Resource binding failed with a server error." }
                throw IllegalStateException("Failed to bind resource")
            }

            classLogger.d { "Sending initial presence stanza." }
            val initialPresenceStanza = XmppPresenceStanza()
            val initialPresenceXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), initialPresenceStanza)
            classLogger.d { "Transmitting presence: $initialPresenceXml" }
            send(Frame.Text(initialPresenceXml))
        }

        classLogger.i { "XMPP Session is now fully active." }
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
                    classLogger.e(exception) { "Error while receiving frame from socket." }
                    throw IllegalStateException("Connection closed while waiting for stanza", exception)
                }
            }

            if (stanzaText != null) {
                classLogger.v { "Handshake received: $stanzaText" }
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
                    classLogger.e(exception) { "Failed to parse stanza during handshake." }
                    throw exception
                }
            }
        }
        throw IllegalStateException("Connection closed before expected stanza received")
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun DefaultClientWebSocketSession.processIncomingLoop() {
        classLogger.d { "Entering main incoming stanza processing loop." }
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
                                    classLogger.d { "[SM] Server requested ACK. Sending h=${smState.inH}" }
                                    val ackStanza = XmppSmAck(h = smState.inH)
                                    val ackXml = xmlFormat.encodeToString(IXmppStreamElement.serializer(), ackStanza)
                                    send(Frame.Text(ackXml))
                                }

                                is XmppSmAck -> {
                                    classLogger.d { "[SM] Server ACK'd h=${decodedElement.h}" }
                                    processAck(decodedElement.h)
                                }

                                is XmppClose -> {
                                    classLogger.i { "Received server-initiated stream close." }
                                    return
                                }

                                is XmppStreamError -> {
                                    classLogger.e { "Received server-initiated stream error: ${decodedElement.text}" }
                                    throw IllegalStateException("Stream Error: ${decodedElement.text}")
                                }

                                is XmppIqStanza -> {
                                    val payload = decodedElement.payload
                                    if (payload is XmppPingPayload && decodedElement.type == "get") {
                                        classLogger.d { "Received Ping from server. Replying Pong." }
                                        val pongStanza = XmppIqStanza(
                                            identifier = decodedElement.identifier,
                                            type = "result",
                                            recipientJid = decodedElement.senderJid,
                                            senderJid = null
                                        )
                                        val pongXml =
                                            xmlFormat.encodeToString(IXmppStreamElement.serializer(), pongStanza)
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
                            classLogger.e(exception) { "Stanza decoding failed in loop." }
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            classLogger.e(exception) { "Incoming loop encountered an exception." }
            if (isActive) throw exception
        } finally {
            classLogger.d { "Exiting main incoming stanza processing loop." }
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
            classLogger.d { "[SM] Removed $count acknowledged stanzas from queue. Remaining: ${smState.unacked.size}" }
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
        classLogger.d { "Starting background keep-alive task." }
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
                        classLogger.w(exception) { "Keep-alive ping failed to send." }
                        break
                    }
                }
            }
        }
    }
}