package com.availelabs.availe.features.appshell

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.core.decompose.asValue
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.features.chat.ChatComponent
import com.availelabs.availe.features.chat.IChatComponent
import com.availelabs.availe.features.dashboard.DashboardComponent
import com.availelabs.availe.features.dashboard.IDashboardComponent
import com.availelabs.availe.features.listingeditor.IListingEditorComponent
import com.availelabs.availe.features.listingeditor.ListingEditorComponent
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.LayoutMode
import kotlinx.coroutines.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IAppShellComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>>
    val chatOverlay: Value<ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>>
    val authenticationState: Value<AuthenticationState>
    val isAuthenticationLoading: Value<Boolean>
    val backDispatcher: BackDispatcher?

    fun onLayoutModeChanged(mode: LayoutMode)
    fun expandChatOverlay()
    fun dismissChatOverlay()
    fun onBackClicked()
    fun onLoginClicked()
    fun onCancelLoginClicked()
    fun onLogoutClicked()
    fun onSettingsClicked()
}

@OptIn(ExperimentalUuidApi::class)
class AppShellComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val initialIntent: AppShellIntent,
    override val backDispatcher: BackDispatcher?,
    override val isAuthenticationLoading: Value<Boolean>,
    private val onExplicitLogin: () -> Unit,
    private val onCancelLogin: () -> Unit,
    private val onShowLoginGuard: () -> Unit
) : IAppShellComponent, ComponentContext by componentContext {

    private val childStackNavigation = StackNavigation<AppShellConfiguration>()
    private val chatOverlayNavigation = SlotNavigation<AppShellConfiguration.Chat>()
    private var layoutMode = LayoutMode.SinglePane

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val authenticationState: Value<AuthenticationState> =
        appContainer.sessionManager.authenticationState.asValue(lifecycle)

    override val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>> = childStack(
        source = childStackNavigation,
        serializer = AppShellConfiguration.serializer(),
        initialStack = { calculateInitialStack(initialIntent) },
        childFactory = ::createChildComponent
    )

    override val chatOverlay: Value<ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>> = childSlot(
        source = chatOverlayNavigation,
        serializer = AppShellConfiguration.Chat.serializer(),
        initialConfiguration = { calculateInitialOverlay(initialIntent) },
        handleBackButton = false,
        childFactory = ::createChatOverlayComponent
    )

    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = childStackNavigation,
        stack = childStack,
        serializer = AppShellConfiguration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
        childSelector = {
            null
        }
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            appContainer.sessionManager.authenticationState.collect { state ->
                if (state !is AuthenticationState.LoggedIn) {
                    dismissChatOverlay()
                }
            }
        }
    }

    override fun onLayoutModeChanged(mode: LayoutMode) {
        if (layoutMode == mode) return
        layoutMode = mode

        if (mode == LayoutMode.SinglePane) {
            val overlayChild = chatOverlay.value.child
            if (overlayChild != null) {
                val configuration = overlayChild.configuration
                chatOverlayNavigation.dismiss()
                childStackNavigation.bringToFront(configuration)
            }
        } else {
            val activeConfiguration = childStack.value.active.configuration
            if (activeConfiguration is AppShellConfiguration.Chat) {
                childStackNavigation.pop()
                chatOverlayNavigation.activate(activeConfiguration)
            }
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun expandChatOverlay() {
        val currentSlot = chatOverlay.value.child
        if (currentSlot != null) {
            val configuration = currentSlot.configuration
            chatOverlayNavigation.dismiss()
            childStackNavigation.bringToFront(configuration)
        }
    }

    override fun dismissChatOverlay() {
        chatOverlayNavigation.dismiss()
    }

    override fun onBackClicked() {
        childStackNavigation.pop()
    }

    override fun onLoginClicked() {
        onExplicitLogin()
    }

    override fun onCancelLoginClicked() {
        onCancelLogin()
    }

    override fun onLogoutClicked() {
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        scope.launch {
            appContainer.authenticationRepository.logout()
        }
    }

    override fun onSettingsClicked() {
        // Settings is now part of ProfileNav inside Dashboard,
        // but if accessed globally, we might need a strategy.
        // For now, assume it's handled via Dashboard tab switching if implemented there.
    }

    private fun calculateInitialStack(intent: AppShellIntent): List<AppShellConfiguration> {
        return when (intent) {
            is AppShellIntent.ListingEditor -> listOf(
                AppShellConfiguration.Dashboard,
                AppShellConfiguration.ListingEditor(intent.listingId)
            )

            is AppShellIntent.Chatting -> listOf(
                AppShellConfiguration.Dashboard,
                AppShellConfiguration.Chat(intent.recipientJid)
            )

            // Everything else is routed to Dashboard to handle internally
            else -> listOf(AppShellConfiguration.Dashboard)
        }
    }

    private fun calculateInitialOverlay(intent: AppShellIntent): AppShellConfiguration.Chat? {
        return when (intent) {
            is AppShellIntent.ViewingListingWithChat -> AppShellConfiguration.Chat(intent.recipientJid)
            else -> null
        }
    }

    private fun createChildComponent(
        configuration: AppShellConfiguration,
        componentContext: ComponentContext
    ): AppShellChildComponent {
        return when (configuration) {
            AppShellConfiguration.Dashboard -> AppShellChildComponent.Dashboard(
                DashboardComponent(
                    componentContext = componentContext,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    messageService = appContainer.messageService,
                    authenticationRepository = appContainer.authenticationRepository,
                    inAppBrowserLauncher = appContainer.inAppBrowserLauncher,
                    onNavigateToListingDetails = { listingId ->
                        // No-op here, Dashboard handles it internally via HomeNav
                    },
                    onNavigateToCreateListing = {
                        childStackNavigation.pushToFront(AppShellConfiguration.ListingEditor(null))
                    },
                    onNavigateToChat = { userId ->
                        // Placeholder
                    },
                    onNavigateToSettings = {
                        // Placeholder
                    },
                    onShowLoginGuard = onShowLoginGuard,
                    initialIntent = initialIntent
                )
            )

            is AppShellConfiguration.Chat -> AppShellChildComponent.Chat(
                ChatComponent(
                    componentContext = componentContext,
                    recipientJid = configuration.recipientJid,
                    sessionManager = appContainer.sessionManager,
                    messagingRepository = appContainer.messagingRepository,
                    onNavigateBack = { childStackNavigation.pop() }
                )
            )

            is AppShellConfiguration.ListingEditor -> AppShellChildComponent.ListingEditor(
                ListingEditorComponent(
                    componentContext = componentContext,
                    listingId = configuration.listingId,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    messageService = appContainer.messageService,
                    onNavigateBack = { childStackNavigation.pop() }
                )
            )
        }
    }

    private fun createChatOverlayComponent(
        configuration: AppShellConfiguration.Chat,
        componentContext: ComponentContext
    ): AppShellChildComponent.Chat {
        return AppShellChildComponent.Chat(
            ChatComponent(
                componentContext = componentContext,
                recipientJid = configuration.recipientJid,
                sessionManager = appContainer.sessionManager,
                messagingRepository = appContainer.messagingRepository,
                onNavigateBack = { chatOverlayNavigation.dismiss() }
            )
        )
    }
}

sealed interface AppShellChildComponent {
    val actions: Value<List<AppBarAction>>

    data class Dashboard(val component: IDashboardComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Chat(val component: IChatComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class ListingEditor(val component: IListingEditorComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }
}