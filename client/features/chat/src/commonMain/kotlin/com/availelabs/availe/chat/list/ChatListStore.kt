package com.availelabs.availe.chat.list

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.chat.list.ChatListStore.*
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.model.ChatConversation
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.repositories.MessagingConnectionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface ChatListStore : Store<Intent, State, Label> {
    sealed interface Intent {
        data class SelectConversation(val jid: Jid) : Intent
    }

    data class State(
        val conversations: List<ChatConversation> = emptyList(),
        val selectedJid: Jid? = null,
        val connectionStatus: MessagingConnectionState = MessagingConnectionState.Disconnected,
        val isLoading: Boolean = false
    )

    sealed interface Label {
        data class ConversationSelected(val jid: Jid) : Label
    }
}

internal class ChatListStoreFactory(
    private val storeFactory: StoreFactory,
    private val messagingRepository: IMessagingRepository,
    private val sessionManager: ISessionRepository
) {
    fun create(): ChatListStore =
        object : ChatListStore, Store<Intent, State, Label> by storeFactory.create(
            name = ChatListStore::class.simpleName,
            initialState = State(),
            bootstrapper = BootstrapperImpl(),
            executorFactory = { ExecutorImpl() },
            reducer = ReducerImpl
        ) {}

    private sealed interface Action {
        data class UpdateConversations(val conversations: List<ChatConversation>) : Action
        data class UpdateConnectionStatus(val status: MessagingConnectionState) : Action
    }

    private sealed interface Message {
        data class SetConversations(val conversations: List<ChatConversation>) : Message
        data class SetConnectionStatus(val status: MessagingConnectionState) : Message
        data class SetSelected(val jid: Jid) : Message
    }

    private inner class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            // 1. Observe Conversations
            scope.launch {
                messagingRepository.getConversations().collectLatest { conversations ->
                    dispatch(Action.UpdateConversations(conversations))
                }
            }

            // 2. Observe Connection Status
            scope.launch {
                messagingRepository.connectionStatus.collectLatest { status ->
                    dispatch(Action.UpdateConnectionStatus(status))
                }
            }

            // 3. Initiate Connection if Logged In
            scope.launch {
                val userProfile = sessionManager.getCurrentUserProfile()
                val authState = sessionManager.authenticationState.value

                if (userProfile != null && authState is AuthenticationState.LoggedIn) {
                    messagingRepository.connect(userProfile.jid, authState.accessToken)
                }
            }
        }
    }

    private class ExecutorImpl : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.UpdateConversations -> dispatch(Message.SetConversations(action.conversations))
                is Action.UpdateConnectionStatus -> dispatch(Message.SetConnectionStatus(action.status))
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.SelectConversation -> {
                    dispatch(Message.SetSelected(intent.jid))
                    publish(Label.ConversationSelected(intent.jid))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State = when (msg) {
            is Message.SetConversations -> copy(conversations = msg.conversations)
            is Message.SetConnectionStatus -> copy(connectionStatus = msg.status)
            is Message.SetSelected -> copy(selectedJid = msg.jid)
        }
    }
}