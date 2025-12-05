package com.availelabs.availe.data.sources.remote

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface IMessagingRemoteDataSource {
    val connectionStatus: StateFlow<String>
    val messages: SharedFlow<String>
    suspend fun connect(userId: String, token: String)
    suspend fun disconnect()
    suspend fun sendMessage(to: String, body: String)
}

class MessagingRemoteDataSource(
    private val httpClient: HttpClient
) : IMessagingRemoteDataSource {

    private val mutableConnectionStatusStateFlow = MutableStateFlow("Disconnected")
    override val connectionStatus: StateFlow<String> = mutableConnectionStatusStateFlow.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    override val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var defaultClientWebSocketSession: DefaultClientWebSocketSession? = null

    private val bodyRegex = Regex("<body>(.*?)</body>")

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
                send(Frame.Text("<open xmlns='urn:ietf:params:xml:ns:xmpp-framing' to='$domain' version='1.0'/>"))

                val authString = "\u0000$userId\u0000$token"
                val authBase64 = Base64.encode(authString.encodeToByteArray())
                send(Frame.Text("<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>$authBase64</auth>"))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.contains("<success")) {
                            mutableConnectionStatusStateFlow.value = "Authenticated"

                            send(Frame.Text("<open xmlns='urn:ietf:params:xml:ns:xmpp-framing' to='$domain' version='1.0'/>"))
                            send(Frame.Text("<iq type='set' id='bind_1'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>availe-app</resource></bind></iq>"))
                            send(Frame.Text("<presence/>"))

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
                            val match = bodyRegex.find(text)
                            if (match != null) {
                                val messageBody = match.groupValues[1]
                                _messages.emit(messageBody)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            mutableConnectionStatusStateFlow.value = "Error: ${e.message}"
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
        val xml = "<message to='$to' type='chat'><body>$body</body></message>"
        defaultClientWebSocketSession?.send(Frame.Text(xml))

        _messages.emit("Me: $body")
    }
}