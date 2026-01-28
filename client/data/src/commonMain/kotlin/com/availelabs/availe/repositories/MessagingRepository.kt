@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.availelabs.availe.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.database.IDatabaseProvider
import com.availelabs.availe.dispatchers.ioDispatcher
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.mapper.toDomain
import com.availelabs.availe.model.*
import com.availelabs.availe.sources.remote.IMessagingRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class MessagingRepository(
    private val messagingRemoteDataSource: IMessagingRemoteDataSource,
    private val databaseProvider: IDatabaseProvider,
    private val sessionManager: ISessionRepository,
    private val scope: CoroutineScope
) : IMessagingRepository {

    override val connectionStatus: StateFlow<MessagingConnectionState> =
        messagingRemoteDataSource.connectionStatus

    init {
        startObservingIncomingMessages()
        startManagingConnection()
    }

    override suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken) {
        messagingRemoteDataSource.connect(jid, authIdentity, token)
    }

    override suspend fun disconnect() {
        messagingRemoteDataSource.disconnect()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid) {
        val database = databaseProvider.getDatabase()
        val messageId = ChatMessageId(Uuid.random().toString())
        val timestamp = Clock.System.now()

        // 1. Optimistic Update: Insert into DB immediately
        database.transaction {
            database.chatQueries.insertMessage(
                id = messageId,
                conversation_jid = to,
                sender_jid = from,
                sender_display_name = null, // Can be updated if we have profile info
                content = body,
                type = MessageType.Text,
                status = MessageStatus.Pending,
                timestamp = timestamp,
                is_mine = true
            )

            // Ensure conversation exists and update last message
            // We need to fetch existing display name to preserve it, or default to JID
            val existingName = database.chatQueries.getConversationDisplayName(to).executeAsOneOrNull()
            val displayName = existingName ?: UserDisplayName(to.value.substringBefore("@"))

            database.chatQueries.upsertConversation(
                jid = to,
                type = ConversationType.OneToOne, // Defaulting to 1:1 for now
                display_name = displayName,
                last_message_content = body,
                last_message_timestamp = timestamp,
                unread_count = UnreadCount(0) // No unread count for sent messages
            )
        }

        // 2. Network Call
        try {
            messagingRemoteDataSource.sendMessage(to, body)
            // 3. Success Confirmation
            database.chatQueries.updateMessageStatus(MessageStatus.Sent, messageId)
        } catch (_: Exception) {
            // 3. Failure Confirmation
            database.chatQueries.updateMessageStatus(MessageStatus.Failed, messageId)
        }
    }

    override suspend fun markAsRead(conversationJid: Jid) {
        val database = databaseProvider.getDatabase()
        database.chatQueries.markAsRead(conversationJid)
    }

    override fun getConversations(): Flow<List<ChatConversation>> {
        return flow {
            val database = databaseProvider.getDatabase()
            emit(database)
        }.flatMapLatest { database ->
            database.chatQueries.selectAllConversations()
                .asFlow()
                .mapToList(ioDispatcher)
                .flatMapLatest { entities ->
                    flow {
                        emit(entities.map { it.toDomain() })
                    }
                }
        }
    }

    override fun getMessages(conversationJid: Jid): Flow<List<ChatMessage>> {
        return flow {
            val database = databaseProvider.getDatabase()
            emit(database)
        }.flatMapLatest { database ->
            database.chatQueries.selectMessagesByJid(conversationJid)
                .asFlow()
                .mapToList(ioDispatcher)
                .flatMapLatest { entities ->
                    flow {
                        emit(entities.map { it.toDomain() })
                    }
                }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun startObservingIncomingMessages() {
        scope.launch(ioDispatcher) {
            messagingRemoteDataSource.incomingMessages.collect { stanza ->
                val fromJidString = stanza.senderJid?.value ?: return@collect
                val bodyString = stanza.body?.value ?: return@collect
                // XMPP JIDs often have resources (user@domain/mobile), strip it for the conversation key
                val bareJidString = fromJidString.substringBefore("/")
                val conversationJid = Jid(bareJidString)
                val senderJid = Jid(fromJidString)
                val content = MessageContent(bodyString)
                val timestamp = Clock.System.now()
                val messageId = ChatMessageId(stanza.identifier?.value ?: Uuid.random().toString())

                val database = databaseProvider.getDatabase()

                database.transaction {
                    // 1. Insert Message
                    database.chatQueries.insertMessage(
                        id = messageId,
                        conversation_jid = conversationJid,
                        sender_jid = senderJid,
                        sender_display_name = null, // Can be populated via Store5/Profile lookup later
                        content = content,
                        type = MessageType.Text,
                        status = MessageStatus.Delivered,
                        timestamp = timestamp,
                        is_mine = false
                    )

                    // 2. Update Conversation & Unread Count
                    val existingUnread = database.chatQueries.getUnreadCount(conversationJid).executeAsOneOrNull()
                    val newUnreadCount = UnreadCount((existingUnread?.value ?: 0) + 1)

                    val existingName =
                        database.chatQueries.getConversationDisplayName(conversationJid).executeAsOneOrNull()
                    val displayName = existingName ?: UserDisplayName(bareJidString.substringBefore("@"))

                    database.chatQueries.upsertConversation(
                        jid = conversationJid,
                        type = ConversationType.OneToOne,
                        display_name = displayName,
                        last_message_content = content,
                        last_message_timestamp = timestamp,
                        unread_count = newUnreadCount
                    )
                }
            }
        }
    }

    private fun startManagingConnection() {
        scope.launch {
            sessionManager.authenticationState.collectLatest { state ->
                when (state) {
                    is AuthenticationState.LoggedIn -> {
                        val userProfile = sessionManager.getCurrentUserProfile()
                        if (userProfile != null) {
                            connect(
                                jid = userProfile.jid,
                                authIdentity = userProfile.id.value.toString(),
                                token = state.accessToken
                            )
                        }
                    }

                    AuthenticationState.LoggedOut,
                    AuthenticationState.SessionExpired,
                    is AuthenticationState.InitializationFailed -> {
                        disconnect()
                    }

                    AuthenticationState.Unknown -> {
                        // Do nothing, wait for initialization
                    }
                }
            }
        }
    }
}