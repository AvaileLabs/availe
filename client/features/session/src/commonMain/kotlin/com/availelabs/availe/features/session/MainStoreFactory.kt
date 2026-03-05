package com.availelabs.availe.features.session

import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.authentication_error_logout
import availe.client.designsystem.generated.resources.authentication_service_unavailable
import availe.client.designsystem.generated.resources.error_session_expired
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.auth.LoginError
import com.availelabs.availe.auth.LoginResult
import com.availelabs.availe.features.session.IMainStore.*
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainStoreFactory(
    private val storeFactory: StoreFactory,
    private val authenticationRepository: IAuthenticationRepository,
    private val sessionManager: ISessionRepository
) {

    fun create(): IMainStore =
        object : IMainStore, Store<Intent, State, Label> by storeFactory.create(
            name = IMainStore::class.simpleName,
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
        data class SetError(val message: UiText?) : Message
    }

    private inner class MainBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                sessionManager.authenticationState.collectLatest { state ->
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
                Intent.CancelLoginRequested -> dispatch(Message.SetLoading(false))
                Intent.ClearErrorRequested -> dispatch(Message.SetError(null))
            }
        }

        private fun handleAuthenticationState(authenticationState: AuthenticationState) {
            when (authenticationState) {
                is AuthenticationState.LoggedIn -> {
                    dispatch(Message.SetAuthenticated(true))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                    dispatch(Message.SetLoading(false))
                }

                is AuthenticationState.LoggedOut -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                }

                is AuthenticationState.SessionExpired -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(true))
                    val message = UiText.ResourceString(Res.string.error_session_expired)
                    dispatch(Message.SetError(message))
                    publish(Label.ErrorOccurred(AppError.AuthenticationError.SessionExpired))
                }

                is AuthenticationState.InitializationFailed -> {
                    dispatch(Message.SetAuthenticated(false))
                    dispatch(Message.SetAuthenticationServiceAvailable(false))
                    val message = UiText.ResourceString(Res.string.authentication_service_unavailable)
                    dispatch(Message.SetError(message))
                }

                AuthenticationState.Unknown -> {
                }
            }
        }

        private fun performLogin() {
            scope.launch {
                dispatch(Message.SetLoading(true))
                when (val loginResult = authenticationRepository.login(sessionManager::updateState)) {
                    is LoginResult.Success -> {
                        dispatch(Message.SetLoading(false))
                    }

                    is LoginResult.Cancelled -> {
                        dispatch(Message.SetLoading(false))
                        publish(Label.InfoOccurred("Login cancelled"))
                    }

                    is LoginResult.Failed -> {
                        dispatch(Message.SetLoading(false))
                        val errorMessage = ErrorMessageMapper.map(loginResult.error)
                        dispatch(Message.SetError(errorMessage))
                        publish(Label.ErrorOccurred(loginResult.error.toAppError()))
                    }
                }
            }
        }

        private fun performLogout() {
            scope.launch {
                dispatch(Message.SetLoading(true))
                try {
                    authenticationRepository.logout()
                } catch (exception: Exception) {
                    val errorMessage = UiText.ResourceString(Res.string.authentication_error_logout)
                    dispatch(Message.SetError(errorMessage))
                    publish(Label.ErrorOccurred(AppError.UnknownError(exception.message)))
                } finally {
                    dispatch(Message.SetLoading(false))
                }
            }
        }

        private fun LoginError.toAppError(): AppError {
            return when (this) {
                LoginError.NoInternetConnection -> AppError.NetworkError.NoInternetConnection
                LoginError.ServerUnreachable -> AppError.NetworkError.ServerUnreachable
                LoginError.InvalidCredentials -> AppError.AuthenticationError.InvalidCredentials
                is LoginError.Unexpected -> AppError.UnknownError(this.debugMessage)
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