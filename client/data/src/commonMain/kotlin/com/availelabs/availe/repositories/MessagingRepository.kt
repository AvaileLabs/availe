@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class, FlowPreview::class)

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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
            .debounce(1000)
            .stateIn(scope, SharingStarted.Eagerly, MessagingConnectionState.Disconnected)

    private val retryTrigger = MutableStateFlow(0)

    init {
        startObservingIncomingMessages()
        startManagingConnection()
        startObservingConnectionErrors()
        startFlushingOnConnection()
    }

    override suspend fun connect(jid: Jid, authIdentity: String, token: AuthToken) {
        messagingRemoteDataSource.connect(jid, authIdentity, token)
    }

    override suspend fun disconnect() {
        messagingRemoteDataSource.disconnect()
    }

    override fun retryConnection() {
        retryTrigger.update { it + 1 }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(to: Jid, body: MessageContent, from: Jid) {
        val database = databaseProvider.getDatabase()
        val messageId = ChatMessageId(Uuid.random().toString())
        val timestamp = Clock.System.now()

        database.transaction {
            database.chatQueries.insertMessage(
                id = messageId,
                conversation_jid = to,
                sender_jid = from,
                sender_display_name = null,
                content = body,
                type = MessageType.Text,
                status = MessageStatus.Pending,
                timestamp = timestamp,
                is_mine = true
            )

            val existingName = database.chatQueries.getConversationDisplayName(to).executeAsOneOrNull()
            val displayName = existingName ?: UserDisplayName(to.value.substringBefore("@"))

            database.chatQueries.upsertConversation(
                jid = to,
                type = ConversationType.OneToOne,
                display_name = displayName,
                last_message_content = body,
                last_message_timestamp = timestamp,
                unread_count = UnreadCount(0)
            )
        }

        try {
            messagingRemoteDataSource.sendMessage(to, body)
            database.chatQueries.updateMessageStatus(MessageStatus.Sent, messageId)
        } catch (exception: Exception) {
            println("MessagingRepository: Failed to send message immediately. Leaving as Pending. Error: ${exception.message}")
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
                if (stanza.type == "error") {
                    println("MessagingRepository: Received error stanza, ignoring: $stanza")
                    return@collect
                }

                val fromJidString = stanza.senderJid?.value ?: return@collect
                val bodyString = stanza.body?.value ?: return@collect
                val bareJidString = fromJidString.substringBefore("/")
                val conversationJid = Jid(bareJidString)
                val senderJid = Jid(fromJidString)
                val content = MessageContent(bodyString)
                val timestamp = Clock.System.now()
                val messageId = ChatMessageId(stanza.identifier?.value ?: Uuid.random().toString())

                val database = databaseProvider.getDatabase()

                database.transaction {
                    database.chatQueries.insertMessage(
                        id = messageId,
                        conversation_jid = conversationJid,
                        sender_jid = senderJid,
                        sender_display_name = null,
                        content = content,
                        type = MessageType.Text,
                        status = MessageStatus.Delivered,
                        timestamp = timestamp,
                        is_mine = false
                    )

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
            combine(
                sessionManager.authenticationState,
                retryTrigger
            ) { state, _ -> state }
                .collectLatest { state ->
                    when (state) {
                        is AuthenticationState.LoggedIn -> {
                            val userProfile = sessionManager.getCurrentUserProfile()
                            if (userProfile != null) {
                                while (isActive) {
                                    try {
                                        connect(
                                            jid = userProfile.jid,
                                            authIdentity = userProfile.id.value.toString(),
                                            token = state.accessToken
                                        )
                                        println("MessagingRepository: Connection closed normally. Reconnecting in 3s...")
                                    } catch (e: Exception) {
                                        println("MessagingRepository: Connection failed with exception: ${e.message}. Reconnecting in 3s...")
                                    }
                                    delay(3000)
                                }
                            }
                        }

                        AuthenticationState.LoggedOut,
                        AuthenticationState.SessionExpired,
                        is AuthenticationState.InitializationFailed -> {
                            disconnect()
                        }

                        AuthenticationState.Unknown -> {
                        }
                    }
                }
        }
    }

    private fun startObservingConnectionErrors() {
        scope.launch {
            connectionStatus
                .filterIsInstance<MessagingConnectionState.AuthenticationFailed>()
                .collectLatest {
                    sessionManager.refreshAccessToken()
                }
        }
    }

    private fun startFlushingOnConnection() {
        scope.launch(ioDispatcher) {
            connectionStatus
                .filter { it == MessagingConnectionState.Authenticated }
                .collectLatest {
                    flushPendingMessages()
                }
        }
    }

    private suspend fun flushPendingMessages() {
        val database = databaseProvider.getDatabase()
        val pendingMessages = withContext(ioDispatcher) {
            database.chatQueries.selectPendingMessages(MessageStatus.Pending).executeAsList()
        }

        if (pendingMessages.isNotEmpty()) {
            println("MessagingRepository: Flushing ${pendingMessages.size} pending messages...")
        }

        pendingMessages.forEach { messageEntity ->
            try {
                messagingRemoteDataSource.sendMessage(
                    to = messageEntity.conversation_jid,
                    body = messageEntity.content
                )
                database.chatQueries.updateMessageStatus(MessageStatus.Sent, messageEntity.id)
                println("MessagingRepository: Successfully flushed message ${messageEntity.id}")
            } catch (exception: Exception) {
                println("MessagingRepository: Failed to flush message ${messageEntity.id}: ${exception.message}")
            }
        }
    }
}