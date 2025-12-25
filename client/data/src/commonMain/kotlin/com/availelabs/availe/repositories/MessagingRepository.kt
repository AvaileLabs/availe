@file:OptIn(ExperimentalTime::class)

package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.sources.remote.IMessagingRemoteDataSource
import kotlinx.coroutines.flow.*
import kotlin.time.ExperimentalTime

internal class MessagingRepository(
    private val messagingRemoteDataSource: IMessagingRemoteDataSource
) : IMessagingRepository {

    override val connectionStatus: StateFlow<MessagingConnectionState> = messagingRemoteDataSource.connectionStatus

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