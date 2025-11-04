package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.di.ApplicationContainer
import com.availelabs.availe.domain.auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface IRootComponent {
    val authenticationService: IAuthenticationService
    val childStack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class Loading(val component: ILoadingComponent) : Child
        data class Login(val component: ILoginComponent) : Child
        data class Main(val component: IMainComponent) : Child
        data class Error(val component: IErrorComponent) : Child
    }
}

class RootComponent(
    componentContext: ComponentContext,
    appContainer: ApplicationContainer
) : IRootComponent, ComponentContext by componentContext {

    private data class InitializationError(val error: Throwable?)

    override val authenticationService: IAuthenticationService = appContainer.authenticationService
    private val navigation = StackNavigation<Config>()

    private val _loginUiState = MutableValue<LoginUiState>(LoginUiState.Idle)
    private val _mainUiState = MutableValue<MainUiState>(MainUiState.Idle)
    private val _initializationError = MutableValue(InitializationError(null))

    override val childStack: Value<ChildStack<*, IRootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Loading,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        val authSub = authenticationService.authenticationState.subscribe(::onAuthStateChanged)
        lifecycle.doOnDestroy {
            authSub.cancel()
        }
    }

    private fun onAuthStateChanged(state: AuthenticationState) {
        when (state) {
            is AuthenticationState.LoggedIn -> {
                _loginUiState.update { LoginUiState.Idle }
                navigation.bringToFront(Config.Main)
            }

            is AuthenticationState.LoggedOut -> {
                _mainUiState.update { MainUiState.Idle }
                navigation.bringToFront(Config.Login)
            }

            is AuthenticationState.Unknown -> navigation.bringToFront(Config.Loading)

            is AuthenticationState.InitializationFailed -> {
                _initializationError.update { InitializationError(state.error) }
                navigation.bringToFront(Config.Error)
            }
        }
    }

    private fun createChild(
        config: Config,
        context: ComponentContext
    ): IRootComponent.Child =
        when (config) {
            is Config.Loading -> IRootComponent.Child.Loading(
                LoadingComponent(context)
            )

            is Config.Login -> IRootComponent.Child.Login(
                LoginComponent(
                    context,
                    uiState = _loginUiState,
                    onLogin = ::onLogin
                )
            )

            is Config.Main -> IRootComponent.Child.Main(
                MainComponent(
                    context,
                    uiState = _mainUiState,
                    onLogout = ::onLogout
                )
            )

            is Config.Error -> IRootComponent.Child.Error(
                ErrorComponent(
                    context,
                    error = _initializationError.value.error
                        ?: IllegalStateException("Error state reached without an error")
                )
            )
        }

    private fun onLogin() {
        scope.launch {
            _loginUiState.update { LoginUiState.Loading }

            when (val result = authenticationService.login()) {
                is LoginResult.Success -> Unit
                is LoginResult.Cancelled -> {
                    _loginUiState.update { LoginUiState.Error("Login cancelled.") }
                }

                is LoginResult.Failed -> {
                    val message = when (result.error) {
                        is LoginError.Authentication -> "Login failed: ${result.error.message}"
                        is LoginError.Storage -> "Login failed: Could not save session."
                        is LoginError.Unknown -> "Login failed: An unexpected error occurred."
                    }
                    _loginUiState.update { LoginUiState.Error(message) }
                }
            }
        }
    }

    private fun onLogout() {
        scope.launch {
            _mainUiState.update { MainUiState.LoggingOut }

            when (val result = authenticationService.logout()) {
                is LogoutResult.Success -> Unit
                is LogoutResult.Cancelled -> Unit
                is LogoutResult.Failed -> {
                    val message = when (result.error) {
                        is LogoutError.Authentication -> "Logout failed: ${result.error.message}"
                        is LogoutError.Storage -> "Logout failed: Could not clear session."
                        is LogoutError.Unknown -> "Logout failed: An unexpected error occurred."
                    }
                    println(message)
                    _mainUiState.update { MainUiState.Idle }
                }
            }
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Loading : Config

        @Serializable
        data object Login : Config

        @Serializable
        data object Main : Config

        @Serializable
        data object Error : Config
    }
}