package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.remote.IMessagingRemoteDataSource
import com.availelabs.availe.domain.model.ChatMessage
import kotlinx.coroutines.flow.*

interface IMessagingRepository {
    val connectionStatus: StateFlow<String>
    val messages: Flow<ChatMessage>
    suspend fun connect(userId: String, token: String)
    suspend fun disconnect()
    suspend fun sendMessage(to: String, body: String)
}

class MessagingRepository(
    private val messagingRemoteDataSource: IMessagingRemoteDataSource
) : IMessagingRepository {

    override val connectionStatus: StateFlow<String> = messagingRemoteDataSource.connectionStatus

    private val outgoingMessages = MutableSharedFlow<ChatMessage>()

    override val messages: Flow<ChatMessage> = merge(
        messagingRemoteDataSource.incomingMessages.map { xmppMessageStanza ->
            ChatMessage(
                content = xmppMessageStanza.body ?: "",
                senderId = xmppMessageStanza.from ?: "Unknown",
                isMine = false
            )
        },
        outgoingMessages
    )

    override suspend fun connect(userId: String, token: String) {
        messagingRemoteDataSource.connect(userId, token)
    }

    override suspend fun disconnect() {
        messagingRemoteDataSource.disconnect()
    }

    override suspend fun sendMessage(to: String, body: String) {
        messagingRemoteDataSource.sendMessage(to, body)
        outgoingMessages.emit(
            ChatMessage(
                content = body,
                senderId = "Me",
                isMine = true
            )
        )
    }
}