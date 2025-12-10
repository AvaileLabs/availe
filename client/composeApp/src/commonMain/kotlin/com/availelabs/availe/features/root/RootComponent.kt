@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.core.decompose.asValue
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.domain.model.DisplayMessage
import com.availelabs.availe.domain.model.ToastId
import com.availelabs.availe.domain.model.ToastMessage
import com.availelabs.availe.features.appshell.*
import com.availelabs.availe.features.login.ILoginComponent
import com.availelabs.availe.features.login.LoginComponent
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IRootComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<RootConfiguration, RootChildComponent>>
    val loginOverlay: Value<ChildSlot<LoginConfiguration, RootChildComponent.Login>>
    val authenticationState: Value<MainStore.State>
    val toastMessages: Value<List<ToastMessage>>
    val backDispatcher: BackDispatcher?
    fun onAuthenticationIntent(intent: MainStore.Intent)
    fun onBackClicked()
    fun showLoginGuard()
    fun dismissLoginOverlay()
    fun dismissToast(id: ToastId)
}

@OptIn(ExperimentalDecomposeApi::class, ExperimentalUuidApi::class)
class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val deepLinkUrl: String? = null
) : IRootComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        appContainer: IApplicationContainer
    ) : this(componentContext, appContainer, null)

    override val backDispatcher: BackDispatcher? = backHandler as? BackDispatcher

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val mainStore = instanceKeeper.getStore {
        MainStoreFactory(
            storeFactory = DefaultStoreFactory(),
            authenticationRepository = appContainer.authenticationRepository,
            sessionManager = appContainer.sessionManager
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val authenticationState: Value<MainStore.State> = mainStore.stateFlow.asValue(lifecycle)

    private val isAuthenticationLoading: Value<Boolean> = authenticationState.map { it.isLoading }

    private val _toastMessages = MutableValue<List<ToastMessage>>(emptyList())
    override val toastMessages: Value<List<ToastMessage>> = _toastMessages

    private val childStackNavigation = StackNavigation<RootConfiguration>()
    private val loginOverlayNavigation = SlotNavigation<LoginConfiguration>()

    override val childStack: Value<ChildStack<RootConfiguration, RootChildComponent>> = childStack(
        source = childStackNavigation,
        serializer = RootConfiguration.serializer(),
        initialStack = {
            listOf(RootNavigationMapper.getInitialIntent(deepLinkUrl))
        },
        childFactory = ::createChildComponent
    )

    override val loginOverlay: Value<ChildSlot<LoginConfiguration, RootChildComponent.Login>> = childSlot(
        source = loginOverlayNavigation,
        serializer = LoginConfiguration.serializer(),
        handleBackButton = true,
        childFactory = { _, ctx ->
            RootChildComponent.Login(
                LoginComponent(
                    componentContext = ctx,
                    onDismiss = { loginOverlayNavigation.dismiss() }
                )
            )
        }
    )

    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = childStackNavigation,
        stack = childStack,
        serializer = RootConfiguration.serializer(),
        pathMapper = { child ->
            when (child.configuration) {
                is RootConfiguration.AppShell -> ""
            }
        },
        childSelector = { child ->
            when (val instance = child.instance) {
                is RootChildComponent.AppShell -> instance.component
                is RootChildComponent.Login -> null
            }
        }
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            mainStore.labels.collect { label ->
                when (label) {
                    is MainStore.Label.ErrorOccurred -> {
                        val toast = ToastMessage.Error(
                            text = DisplayMessage(label.message)
                        )
                        _toastMessages.update { it + toast }

                        scope.launch {
                            delay(toast.durationMilliseconds)
                            dismissToast(toast.id)
                        }
                    }
                }
            }
        }
    }

    override fun onAuthenticationIntent(intent: MainStore.Intent) {
        mainStore.accept(intent)
    }

    override fun onBackClicked() {
        childStackNavigation.pop()
    }

    override fun showLoginGuard() {
        loginOverlayNavigation.activate(LoginConfiguration)
    }

    override fun dismissLoginOverlay() {
        loginOverlayNavigation.dismiss()
    }

    override fun dismissToast(id: ToastId) {
        _toastMessages.update { list -> list.filter { it.id != id } }
    }

    private fun createChildComponent(
        configuration: RootConfiguration,
        componentContext: ComponentContext
    ): RootChildComponent {
        return when (configuration) {
            is RootConfiguration.AppShell -> {
                RootChildComponent.AppShell(
                    AppShellComponent(
                        componentContext = componentContext,
                        appContainer = appContainer,
                        initialIntent = configuration.intent,
                        backDispatcher = backDispatcher,
                        isAuthenticationLoading = isAuthenticationLoading,
                        onExplicitLogin = { mainStore.accept(MainStore.Intent.LoginRequested) },
                        onCancelLogin = { mainStore.accept(MainStore.Intent.CancelLoginRequested) },
                        onShowLoginGuard = { loginOverlayNavigation.activate(LoginConfiguration) }
                    )
                )
            }
        }
    }
}

sealed interface RootChildComponent {
    data class AppShell(val component: IAppShellComponent) : RootChildComponent
    data class Login(val component: ILoginComponent) : RootChildComponent
}

@Serializable
sealed interface RootConfiguration {
    @Serializable
    data class AppShell(
        val intent: AppShellIntent
    ) : RootConfiguration
}

@Serializable
data object LoginConfiguration