package com.availelabs.availe.features.main

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.data.repositories.IAuthenticationRepository
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.features.main.MainStore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface MainStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object LoginRequested : Intent
        data object LogoutRequested : Intent
        data object ClearErrorRequested : Intent
    }

    data class State(
        val isAuthenticated: Boolean = false,
        val isAuthenticationServiceAvailable: Boolean = true,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Label {
        data class ErrorOccurred(val message: String) : Label
    }
}

class MainStoreFactory(
    private val storeFactory: StoreFactory,
    private val authenticationRepository: IAuthenticationRepository,
    private val sessionManager: ISessionManager
) {

    fun create(): MainStore =
        object : MainStore, Store<Intent, State, Label> by storeFactory.create(
            name = MainStore::class.simpleName,
            initialState = State(),
            bootstrapper = MainBootstrapper(),
            executorFactory = { MainExecutor() },
            reducer = MainReducer
        ) {}

    private sealed interface Action {
        data class UpdateAuthenticationState(val authenticationState: AuthenticationState) : Action
    }

    private sealed interface Message {
        data class SetLoading(val isLoading: Boolean) : Message
        data class SetAuthenticated(val isAuthenticated: Boolean) : Message
        data class SetAuthenticationServiceAvailable(val isAvailable: Boolean) : Message
        data class SetError(val message: String?) : Message
    }

    private inner class MainBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                val authenticationFlow = callbackFlow {
                    val subscription = sessionManager.authenticationState.subscribe { state ->
                        trySend(state)
                    }
                    awaitClose { subscription.cancel() }
                }

                authenticationFlow.collectLatest { state ->
                    dispatch(Action.UpdateAuthenticationState(state))
                }
            }
        }
    }

    private inner class MainExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.UpdateAuthenticationState -> handleAuthenticationState(action.authenticationState)
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.LoginRequested -> performLogin()
                Intent.LogoutRequested -> performLogout()
                Intent.ClearErrorRequested -> dispatch(Message.SetError(null))
            }
        }

        private fun handleAuthenticationState(authenticationState: AuthenticationState) {
            when (authenticationState) {
                is AuthenticationState.LoggedIn -> {
                    dispatch(Message.SetAuthenticated(true))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                }

                is AuthenticationState.LoggedOut -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                }

                is AuthenticationState.SessionExpired -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                    val message = "Session expired. Please log in again."
                    dispatch(Message.SetError(message))
                    publish(Label.ErrorOccurred(message))
                }

                is AuthenticationState.InitializationFailed -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(false))
                    val message = "Authentication service unavailable."
                    dispatch(Message.SetError(message))
                }

                AuthenticationState.Unknown -> {
                }
            }
        }

        private fun performLogin() {
            scope.launch {
                dispatch(Message.SetLoading(true))
                try {
                    authenticationRepository.login(sessionManager::updateState)
                } catch (_: Exception) {
                    val errorMessage = "Cannot connect to authentication server. Please try again later."
                    dispatch(Message.SetError(errorMessage))
                    publish(Label.ErrorOccurred(errorMessage))
                } finally {
                    dispatch(Message.SetLoading(false))
                }
            }
        }

        private fun performLogout() {
            scope.launch {
                dispatch(Message.SetLoading(true))
                try {
                    authenticationRepository.logout()
                } catch (exception: Exception) {
                    val errorMessage = exception.message ?: "Logout failed"
                    dispatch(Message.SetError(errorMessage))
                    publish(Label.ErrorOccurred(errorMessage))
                } finally {
                    dispatch(Message.SetLoading(false))
                }
            }
        }
    }

    private object MainReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.SetLoading -> copy(isLoading = msg.isLoading)
                is Message.SetAuthenticated -> copy(isAuthenticated = msg.isAuthenticated)
                is Message.SetAuthenticationServiceAvailable -> copy(isAuthenticationServiceAvailable = msg.isAvailable)
                is Message.SetError -> copy(errorMessage = msg.message)
            }
    }
}