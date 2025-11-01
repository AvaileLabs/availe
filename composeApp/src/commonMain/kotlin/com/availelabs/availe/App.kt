package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.auth.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@Composable
fun App(root: IRootComponent) {
    Children(
        stack = root.childStack,
    ) {
        when (val child = it.instance) {
            is IRootComponent.Child.Login -> LoginScreen(child.component)
            is IRootComponent.Child.Main -> MainScreen(child.component)
        }
    }
}

interface IRootComponent {
    val authService: AuthService
    val childStack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class Login(val component: LoginComponent) : Child
        data class Main(val component: MainComponent) : Child
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
class RootComponent(
    componentContext: ComponentContext,
    codeAuthFlowFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore
) : IRootComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val navigation = StackNavigation<Config>()

    override val authService = AuthService(
        authFactory = codeAuthFlowFactory,
        tokenStore = tokenStore,
        coroutineScope = scope
    )

    override val childStack: Value<ChildStack<*, IRootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Login,
            handleBackButton = true,
            childFactory = ::createChild
        )

    init {
        val authSub = authService.authState.subscribe(::onAuthStateChanged)

        lifecycle.doOnDestroy {
            authSub.cancel()
            scope.coroutineContext[Job]?.cancel()
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
                DefaultLoginComponent(context, onLogin = ::onLogin)
            )

            is Config.Main -> IRootComponent.Child.Main(
                DefaultMainComponent(context, onLogout = ::onLogout)
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