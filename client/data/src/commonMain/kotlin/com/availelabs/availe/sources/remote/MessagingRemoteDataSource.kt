package com.availelabs.availe.sources.remote

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.dto.*
import com.availelabs.availe.repositories.MessagingConnectionState
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal interface IMessagingRemoteDataSource {
    val connectionStatus: StateFlow<MessagingConnectionState>
    val incomingMessages: SharedFlow<XmppMessageStanza>
    suspend fun connect(jid: Jid, token: AuthToken)
    suspend fun disconnect()
    suspend fun sendMessage(to: Jid, body: MessageContent)
}

internal class MessagingRemoteDataSource(
    private val httpClient: HttpClient
) : IMessagingRemoteDataSource {

    private val mutableConnectionStatusStateFlow =
        MutableStateFlow<MessagingConnectionState>(MessagingConnectionState.Disconnected)
    override val connectionStatus: StateFlow<MessagingConnectionState> = mutableConnectionStatusStateFlow.asStateFlow()

    private val mutableIncomingMessagesSharedFlow = MutableSharedFlow<XmppMessageStanza>()
    override val incomingMessages: SharedFlow<XmppMessageStanza> = mutableIncomingMessagesSharedFlow.asSharedFlow()

    private var defaultClientWebSocketSession: DefaultClientWebSocketSession? = null

    private val xmlSerializer = XML {
        autoPolymorphic = true
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun connect(jid: Jid, token: AuthToken) {
        try {
            mutableConnectionStatusStateFlow.value = MessagingConnectionState.Connecting
            httpClient.ws(
                method = HttpMethod.Get,
                host = NetworkConstants.HOST_IP,
                port = SharedNetworkConstants.EJABBERD_ADMIN_PORT,
                path = "/ws"
            ) {
                defaultClientWebSocketSession = this
                mutableConnectionStatusStateFlow.value = MessagingConnectionState.Handshaking

                val domain = SharedNetworkConstants.XMPP_DOMAIN
                val openStanza = XmppOpenStanza(to = domain, version = "1.0")
                send(Frame.Text(xmlSerializer.encodeToString(XmppOpenStanza.serializer(), openStanza)))

                val authString = "\u0000${jid.value}\u0000${token.value}"
                val authBase64 = Base64.encode(authString.encodeToByteArray())
                val authStanza = XmppAuthStanza(mechanism = "PLAIN", content = authBase64)
                send(Frame.Text(xmlSerializer.encodeToString(XmppAuthStanza.serializer(), authStanza)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.contains("<success")) {
                            mutableConnectionStatusStateFlow.value = MessagingConnectionState.Authenticated

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
                            mutableConnectionStatusStateFlow.value = MessagingConnectionState.AuthenticationFailed
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
                                    mutableIncomingMessagesSharedFlow.emit(messageStanza)
                                } catch (exception: Exception) {
                                    println("Failed to parse message: ${exception.message}")
                                }
                            }
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            mutableConnectionStatusStateFlow.value =
                MessagingConnectionState.Error(exception.message ?: "Unknown error")
        } finally {
            if (mutableConnectionStatusStateFlow.value !is MessagingConnectionState.Error) {
                mutableConnectionStatusStateFlow.value = MessagingConnectionState.Disconnected
            }
            defaultClientWebSocketSession = null
        }
    }

    override suspend fun disconnect() {
        defaultClientWebSocketSession?.close()
        defaultClientWebSocketSession = null
        mutableConnectionStatusStateFlow.value = MessagingConnectionState.Disconnected
    }

    override suspend fun sendMessage(to: Jid, body: MessageContent) {
        val messageStanza = XmppMessageStanza(to = to.value, type = "chat", body = body.value)
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