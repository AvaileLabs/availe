package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.model.*
import com.availelabs.availe.sources.remote.IMessagingRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class MessagingRepository(
    private val messagingRemoteDataSource: IMessagingRemoteDataSource
) : IMessagingRepository {

    override val connectionStatus: StateFlow<MessagingConnectionState> =
        messagingRemoteDataSource.connectionStatus

    @OptIn(ExperimentalUuidApi::class)
    override val messages: Flow<ChatMessage> = messagingRemoteDataSource.incomingMessages.map { stanza ->
        ChatMessage(
            id = ChatMessageId(stanza.identifier?.value ?: Uuid.random().toString()),
            content = MessageContent(stanza.body?.value ?: ""),
            senderJid = Jid(stanza.senderJid?.value ?: ""),
            isMine = false,
            timestamp = Clock.System.now()
        )
    }

    override suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken) {
        messagingRemoteDataSource.connect(jid, authIdentity, token)
    }

    override suspend fun disconnect() {
        messagingRemoteDataSource.disconnect()
    }

    override suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid) {
        messagingRemoteDataSource.sendMessage(to, body)
    }

    override fun getConversations(): Flow<List<ChatConversation>> = flow {
        emit(generateMockConversations())
    }

    private fun generateMockConversations(): List<ChatConversation> {
        val now = Clock.System.now()

        return listOf(
            ChatConversation(
                jid = Jid("alice@availe.com"),
                displayName = UserDisplayName("Alice Wonderland"),
                avatarUrl = ImageUrl("https://i.pravatar.cc/150?u=alice"),
                lastMessage = MessageContent("Hey, is the item still available?"),
                lastMessageTimestamp = LastMessageTimestamp(now.minus(5.minutes)),
                unreadCount = UnreadCount(2),
                isOnline = IsOnline(true)
            ),
            ChatConversation(
                jid = Jid("bob@availe.com"),
                displayName = UserDisplayName("Bob Builder"),
                avatarUrl = ImageUrl("https://i.pravatar.cc/150?u=bob"),
                lastMessage = MessageContent("Thanks for the quick reply!"),
                lastMessageTimestamp = LastMessageTimestamp(now.minus(60.minutes)),
                unreadCount = UnreadCount(0),
                isOnline = IsOnline(false)
            ),
            ChatConversation(
                jid = Jid("charlie@availe.com"),
                displayName = UserDisplayName("Charlie Chaplin"),
                avatarUrl = null,
                lastMessage = MessageContent("Can we meet tomorrow?"),
                lastMessageTimestamp = LastMessageTimestamp(now.minus(120.minutes)),
                unreadCount = UnreadCount(1),
                isOnline = IsOnline(true)
            ),
            ChatConversation(
                jid = Jid("david@availe.com"),
                displayName = UserDisplayName("David Bowie"),
                avatarUrl = ImageUrl("https://i.pravatar.cc/150?u=david"),
                lastMessage = MessageContent("I'll take it."),
                lastMessageTimestamp = LastMessageTimestamp(now.minus(300.minutes)),
                unreadCount = UnreadCount(0),
                isOnline = IsOnline(false)
            ),
            ChatConversation(
                jid = Jid("eve@availe.com"),
                displayName = UserDisplayName("Eve Polastri"),
                avatarUrl = ImageUrl("https://i.pravatar.cc/150?u=eve"),
                lastMessage = MessageContent("Interesting offer."),
                lastMessageTimestamp = LastMessageTimestamp(now.minus(1000.minutes)),
                unreadCount = UnreadCount(5),
                isOnline = IsOnline(true)
            )
        )
    }
}