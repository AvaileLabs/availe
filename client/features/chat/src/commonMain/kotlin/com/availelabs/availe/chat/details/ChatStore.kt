@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.chat.details

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.chat.details.ChatStore.*
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.repositories.MessagingConnectionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface ChatStore : Store<Intent, State, Label> {
    sealed interface Intent {
        data class SendMessage(val body: MessageContent) : Intent
    }

    data class State(
        val recipientJid: Jid,
        val connectionStatus: MessagingConnectionState = MessagingConnectionState.Disconnected,
        val messages: List<ChatMessage> = emptyList(),
        val isOnline: Boolean = false
    )

    sealed interface Label
}

internal class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val messagingRepository: IMessagingRepository,
    private val sessionManager: ISessionRepository,
    private val recipientJid: Jid
) {
    fun create(): ChatStore =
        object : ChatStore, Store<Intent, State, Label> by storeFactory.create(
            name = ChatStore::class.simpleName,
            initialState = State(recipientJid = recipientJid),
            bootstrapper = ChatBootstrapper(),
            executorFactory = { ChatExecutor() },
            reducer = ChatReducer
        ) {}

    private sealed interface Action {
        data class UpdateStatus(val status: MessagingConnectionState) : Action
        data class MessagesUpdated(val messages: List<ChatMessage>) : Action
    }

    private sealed interface Message {
        data class SetStatus(val status: MessagingConnectionState) : Message
        data class SetMessages(val messages: List<ChatMessage>) : Message
    }

    private inner class ChatBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                messagingRepository.connectionStatus.collectLatest { status ->
                    dispatch(Action.UpdateStatus(status))
                }
            }

            scope.launch {
                messagingRepository.getMessages(recipientJid).collectLatest { messages ->
                    if (messages.isNotEmpty()) {
                        messagingRepository.markAsRead(recipientJid)
                    }
                    dispatch(Action.MessagesUpdated(messages))
                }
            }
        }
    }

    private inner class ChatExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.UpdateStatus -> dispatch(Message.SetStatus(action.status))
                is Action.MessagesUpdated -> dispatch(Message.SetMessages(action.messages))
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.SendMessage -> sendMessage(intent.body)
            }
        }

        private fun sendMessage(content: MessageContent) {
            scope.launch {
                val userProfile = sessionManager.getCurrentUserProfile() ?: return@launch
                val userJid = userProfile.jid
                messagingRepository.sendMessage(recipientJid, content, userJid)
            }
        }
    }

    private object ChatReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State = when (msg) {
            is Message.SetStatus -> copy(connectionStatus = msg.status)
            is Message.SetMessages -> copy(messages = msg.messages)
        }
    }
}