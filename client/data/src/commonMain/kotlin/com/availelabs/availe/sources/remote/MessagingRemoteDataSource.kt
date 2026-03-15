package com.availelabs.availe.sources.remote

import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.network.XmppEndpoint
import com.availelabs.availe.repositories.MessagingConnectionState
import com.availelabs.availe.xmpp.dto.XmppMessageStanza
import com.availelabs.availe.xmpp.model.MessageBody
import com.availelabs.availe.xmpp.model.XmppJid
import com.availelabs.availe.xmpp.session.IXmppSession
import com.availelabs.availe.xmpp.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

internal interface IMessagingRemoteDataSource {
    val connectionStatus: StateFlow<MessagingConnectionState>
    val incomingMessages: Flow<XmppMessageStanza>
    suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken)
    suspend fun disconnect()
    suspend fun sendMessage(to: Jid, body: MessageContent)
}

internal class MessagingRemoteDataSource(
    private val xmppSession: IXmppSession,
    private val xmppEndpoint: XmppEndpoint,
) : IMessagingRemoteDataSource {

    private val scope = CoroutineScope(Dispatchers.Main)

    override val connectionStatus: StateFlow<MessagingConnectionState> = xmppSession.sessionState
        .map { sessionState ->
            val mappedState = mapSessionState(sessionState)
            println("MessagingRemoteDataSource: Mapping session state $sessionState to connection state $mappedState")
            mappedState
        }
        .stateIn(scope, SharingStarted.Eagerly, MessagingConnectionState.Disconnected)

    override val incomingMessages = xmppSession.incomingStanzas
        .filterIsInstance<XmppMessageStanza>()
        .onEach { println("MessagingRemoteDataSource: Received incoming message stanza from ${it.senderJid?.value}") }
        .shareIn(scope, SharingStarted.WhileSubscribed())

    override suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken) {
        println("MessagingRemoteDataSource: Initiating connection request for user: ${jid.value}")
        xmppSession.connect(
            xmppJid = XmppJid(jid.value),
            authIdentity = authIdentity,
            password = token.value,
            host = xmppEndpoint.hostAddress.value,
            port = xmppEndpoint.port,
        )
    }

    override suspend fun disconnect() {
        println("MessagingRemoteDataSource: Initiating explicit disconnect request.")
        xmppSession.disconnect()
    }

    override suspend fun sendMessage(to: Jid, body: MessageContent) {
        println("MessagingRemoteDataSource: Preparing to send message to: ${to.value}")
        val stanza = XmppMessageStanza(
            recipientJid = XmppJid(to.value),
            body = MessageBody(body.value),
            type = "chat"
        )
        try {
            xmppSession.sendStanza(stanza)
        } catch (e: IllegalStateException) {
            println("MessagingRemoteDataSource: Attempted to send message while disconnected: ${e.message}")
            // We ignore the error here. The Repository keeps the message as Pending.
            // When the session reconnects, XEP-0198 or the Repository flush logic will handle it.
        }
    }

    private fun mapSessionState(sessionState: SessionState): MessagingConnectionState {
        return when (sessionState) {
            is SessionState.Disconnected -> MessagingConnectionState.Disconnected
            is SessionState.Connecting -> MessagingConnectionState.Connecting
            is SessionState.Negotiating -> MessagingConnectionState.Handshaking
            is SessionState.Authenticating -> MessagingConnectionState.Handshaking
            is SessionState.Binding -> MessagingConnectionState.Handshaking
            is SessionState.Active -> MessagingConnectionState.Authenticated
            is SessionState.Failed -> {
                val message = sessionState.reason
                if (message.contains("Authentication Failed", ignoreCase = true)) {
                    MessagingConnectionState.AuthenticationFailed
                } else {
                    MessagingConnectionState.Error(message)
                }
            }
        }
    }
}