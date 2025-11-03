package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.data.auth.IAuthService
import com.availelabs.availe.di.IAppContainer
import com.availelabs.availe.domain.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface IRootComponent {
    val authService: IAuthService
    val childStack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class Login(val component: ILoginComponent) : Child
        data class Main(val component: IMainComponent) : Child
    }
}

class RootComponent(
    componentContext: ComponentContext,
    appContainer: IAppContainer
) : IRootComponent, ComponentContext by componentContext {

    override val authService: IAuthService = appContainer.authService
    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, IRootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Login,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        val authSub = authService.authState.subscribe(::onAuthStateChanged)
        lifecycle.doOnDestroy {
            authSub.cancel()
        }
    }

    private fun onAuthStateChanged(state: AuthState) {
        when (state) {
            is AuthState.LoggedIn -> navigation.bringToFront(Config.Main)
            is AuthState.LoggedOut -> navigation.bringToFront(Config.Login)
            is AuthState.Unknown -> Unit
        }
    }

    private fun createChild(
        config: Config,
        context: ComponentContext
    ): IRootComponent.Child =
        when (config) {
            is Config.Login -> IRootComponent.Child.Login(
                LoginComponent(context, onLogin = ::onLogin)
            )

            is Config.Main -> IRootComponent.Child.Main(
                MainComponent(context, onLogout = ::onLogout)
            )
        }

    private fun onLogin() {
        scope.launch {
            authService.login()
        }
    }

    private fun onLogout() {
        scope.launch {
            authService.logout()
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Login : Config

        @Serializable
        data object Main : Config
    }
}