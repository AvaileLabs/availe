package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.AuthToken
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatConversation
import com.availelabs.availe.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface MessagingConnectionState {
    data object Disconnected : MessagingConnectionState
    data object Connecting : MessagingConnectionState
    data object Handshaking : MessagingConnectionState
    data object Authenticated : MessagingConnectionState
    data object AuthenticationFailed : MessagingConnectionState
    data class Error(val message: String) : MessagingConnectionState
}

interface IMessagingRepository {
    val connectionStatus: StateFlow<MessagingConnectionState>

    suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken)
    suspend fun disconnect()
    fun retryConnection()

    suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid)

    suspend fun markAsRead(conversationJid: Jid)

    fun getConversations(): Flow<List<ChatConversation>>

    fun getMessages(conversationJid: Jid): Flow<List<ChatMessage>>
}