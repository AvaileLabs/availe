package com.availelabs.availe.data.sources.remote

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.data.dto.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface IMessagingRemoteDataSource {
    val connectionStatus: StateFlow<String>
    val incomingMessages: SharedFlow<XmppMessageStanza>
    suspend fun connect(userId: String, token: String)
    suspend fun disconnect()
    suspend fun sendMessage(to: String, body: String)
}

class MessagingRemoteDataSource(
    private val httpClient: HttpClient
) : IMessagingRemoteDataSource {

    private val mutableConnectionStatusStateFlow = MutableStateFlow("Disconnected")
    override val connectionStatus: StateFlow<String> = mutableConnectionStatusStateFlow.asStateFlow()

    private val mutableIncomingMessagesSharedFlow = MutableSharedFlow<XmppMessageStanza>()
    override val incomingMessages: SharedFlow<XmppMessageStanza> = mutableIncomingMessagesSharedFlow.asSharedFlow()

    private var defaultClientWebSocketSession: DefaultClientWebSocketSession? = null

    private val xmlSerializer = XML {
        autoPolymorphic = true
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun connect(userId: String, token: String) {
        try {
            mutableConnectionStatusStateFlow.value = "Connecting..."
            httpClient.ws(
                method = HttpMethod.Get,
                host = NetworkConstants.HOST_IP,
                port = SharedNetworkConstants.EJABBERD_ADMIN_PORT,
                path = "/ws"
            ) {
                defaultClientWebSocketSession = this
                mutableConnectionStatusStateFlow.value = "Connected (Handshaking)"

                val domain = "localhost"
                val openStanza = XmppOpenStanza(to = domain, version = "1.0")
                send(Frame.Text(xmlSerializer.encodeToString(XmppOpenStanza.serializer(), openStanza)))

                val authString = "\u0000$userId\u0000$token"
                val authBase64 = Base64.encode(authString.encodeToByteArray())
                val authStanza = XmppAuthStanza(mechanism = "PLAIN", content = authBase64)
                send(Frame.Text(xmlSerializer.encodeToString(XmppAuthStanza.serializer(), authStanza)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.contains("<success")) {
                            mutableConnectionStatusStateFlow.value = "Authenticated"

                            send(Frame.Text(xmlSerializer.encodeToString(XmppOpenStanza.serializer(), openStanza)))

                            val bindIqStanza = XmppIqStanza(
                                type = "set",
                                id = "bind_1",
                                bind = XmppBindStanza()
                            )
                            send(Frame.Text(xmlSerializer.encodeToString(XmppIqStanza.serializer(), bindIqStanza)))

                            val presenceStanza = XmppPresenceStanza()
                            send(
                                Frame.Text(
                                    xmlSerializer.encodeToString(
                                        XmppPresenceStanza.serializer(),
                                        presenceStanza
                                    )
                                )
                            )

                            break
                        } else if (text.contains("<failure")) {
                            mutableConnectionStatusStateFlow.value = "Authentication Failed"
                            close()
                            return@ws
                        }
                    }
                }

                while (isActive) {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            if (text.contains("<message")) {
                                try {
                                    val messageStanza =
                                        xmlSerializer.decodeFromString(XmppMessageStanza.serializer(), text)
                                    if (messageStanza.body != null) {
                                        mutableIncomingMessagesSharedFlow.emit(messageStanza)
                                    }
                                } catch (exception: Exception) {
                                    println("Failed to parse message: ${exception.message}")
                                }
                            }
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            mutableConnectionStatusStateFlow.value = "Error: ${exception.message}"
        } finally {
            if (!mutableConnectionStatusStateFlow.value.startsWith("Error")) {
                mutableConnectionStatusStateFlow.value = "Disconnected"
            }
            defaultClientWebSocketSession = null
        }
    }

    override suspend fun disconnect() {
        defaultClientWebSocketSession?.close()
        defaultClientWebSocketSession = null
        mutableConnectionStatusStateFlow.value = "Disconnected"
    }

    override suspend fun sendMessage(to: String, body: String) {
        val messageStanza = XmppMessageStanza(to = to, type = "chat", body = body)
        defaultClientWebSocketSession?.send(
            Frame.Text(
                xmlSerializer.encodeToString(
                    XmppMessageStanza.serializer(),
                    messageStanza
                )
            )
        )
    }
}