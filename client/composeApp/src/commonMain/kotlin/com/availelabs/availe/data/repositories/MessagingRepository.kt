package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.remote.IMessagingRemoteDataSource
import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.domain.model.ChatMessage
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import kotlinx.coroutines.flow.*

interface IMessagingRepository {
    val connectionStatus: StateFlow<String>
    val messages: Flow<ChatMessage>
    suspend fun connect(jid: Jid, token: AuthToken)
    suspend fun disconnect()
    suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid)
}

class MessagingRepository(
    private val messagingRemoteDataSource: IMessagingRemoteDataSource
) : IMessagingRepository {

    override val connectionStatus: StateFlow<String> = messagingRemoteDataSource.connectionStatus

    private val outgoingMessages = MutableSharedFlow<ChatMessage>()

    override val messages: Flow<ChatMessage> = merge(
        messagingRemoteDataSource.incomingMessages.mapNotNull { xmppMessageStanza ->
            val from = xmppMessageStanza.from
            val body = xmppMessageStanza.body

            if (from != null && body != null) {
                ChatMessage(
                    content = MessageContent(body),
                    senderJid = Jid(from),
                    isMine = false
                )
            } else {
                null
            }
        },
        outgoingMessages
    )

    override suspend fun connect(jid: Jid, token: AuthToken) {
        messagingRemoteDataSource.connect(jid, token)
    }

    override suspend fun disconnect() {
        messagingRemoteDataSource.disconnect()
    }

    override suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid) {
        messagingRemoteDataSource.sendMessage(to, body)
        outgoingMessages.emit(
            ChatMessage(
                content = body,
                senderJid = from,
                isMine = true
            )
        )
    }
}