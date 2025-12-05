@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.chat

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.data.sources.remote.IMessagingRemoteDataSource
import com.availelabs.availe.features.chat.ChatStore.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface ChatStore : Store<Intent, State, Label> {
    sealed interface Intent {
        data object Connect : Intent
        data object Disconnect : Intent
        data class SendMessage(val body: String) : Intent
    }

    data class State(
        val connectionStatus: String = "Disconnected",
        val messages: List<String> = emptyList()
    )

    sealed interface Label
}

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val messagingRemoteDataSource: IMessagingRemoteDataSource,
    private val sessionManager: ISessionManager,
    private val recipientId: String
) {
    fun create(): ChatStore =
        object : ChatStore, Store<Intent, State, Label> by storeFactory.create(
            name = ChatStore::class.simpleName,
            initialState = State(),
            bootstrapper = ChatBootstrapper(),
            executorFactory = { ChatExecutor() },
            reducer = ChatReducer
        ) {}

    private sealed interface Action {
        data class UpdateStatus(val status: String) : Action
        data class MessageReceived(val message: String) : Action
    }

    private sealed interface Message {
        data class SetStatus(val status: String) : Message
        data class AddMessage(val message: String) : Message
    }

    private inner class ChatBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                messagingRemoteDataSource.connectionStatus.collectLatest { status ->
                    dispatch(Action.UpdateStatus(status))
                }
            }
            scope.launch {
                messagingRemoteDataSource.messages.collectLatest { msg ->
                    dispatch(Action.MessageReceived(msg))
                }
            }
        }
    }

    private inner class ChatExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.UpdateStatus -> dispatch(Message.SetStatus(action.status))
                is Action.MessageReceived -> dispatch(Message.AddMessage(action.message))
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.Connect -> connect()
                Intent.Disconnect -> disconnect()
                is Intent.SendMessage -> sendMessage(intent.body)
            }
        }

        private fun connect() {
            scope.launch {
                messagingRemoteDataSource.connect("test", "password")

                /*
                val state = sessionManager.authenticationState.value
                if (state is AuthenticationState.LoggedIn) {
                    val userProfile = sessionManager.getCurrentUserProfile()
                    if (userProfile != null) {
                        messagingRemoteDataSource.connect(userProfile.id.value.toString(), state.accessToken)
                    }
                }
                */
            }
        }

        private fun disconnect() {
            scope.launch {
                messagingRemoteDataSource.disconnect()
            }
        }

        private fun sendMessage(body: String) {
            scope.launch {
                messagingRemoteDataSource.sendMessage("$recipientId@localhost", body)
            }
        }
    }

    private object ChatReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State = when (msg) {
            is Message.SetStatus -> copy(connectionStatus = msg.status)
            is Message.AddMessage -> copy(messages = messages + msg.message)
        }
    }
}