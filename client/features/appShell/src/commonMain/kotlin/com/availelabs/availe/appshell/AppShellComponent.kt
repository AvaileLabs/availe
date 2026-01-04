package com.availelabs.availe.appshell

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.chat.details.ChatComponent
import com.availelabs.availe.chat.details.IChatComponent
import com.availelabs.availe.chat.root.ChatRootComponent
import com.availelabs.availe.chat.root.IChatRootComponent
import com.availelabs.availe.dashboard.DashboardComponent
import com.availelabs.availe.dashboard.DashboardConfiguration
import com.availelabs.availe.dashboard.IDashboardComponent
import com.availelabs.availe.decompose.asValue
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.listingeditor.IListingEditorComponent
import com.availelabs.availe.listingeditor.ListingEditorComponent
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.LayoutMode
import kotlinx.coroutines.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IAppShellComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>>
    val chatOverlay: Value<ChildSlot<AppShellConfiguration.ChatOverlay, AppShellChildComponent.ChatOverlay>>
    val authenticationState: Value<AuthenticationState>
    val isAuthenticationLoading: Value<Boolean>
    val showBackArrow: Value<Boolean>
    val shouldShowChatOverlay: Value<Boolean>
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
    private val chatOverlayNavigation = SlotNavigation<AppShellConfiguration.ChatOverlay>()
    private var layoutMode = LayoutMode.SinglePane

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val authenticationState: Value<AuthenticationState> =
        appContainer.sessionManager.authenticationState.asValue(lifecycle)

    override val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>> = childStack(
        source = childStackNavigation,
        serializer = AppShellConfiguration.serializer(),
        initialStack = { calculateInitialStack(initialIntent) },
        handleBackButton = true,
        childFactory = ::createChildComponent
    )

    override val chatOverlay: Value<ChildSlot<AppShellConfiguration.ChatOverlay, AppShellChildComponent.ChatOverlay>> =
        childSlot(
            source = chatOverlayNavigation,
            serializer = AppShellConfiguration.ChatOverlay.serializer(),
            initialConfiguration = { null },
            handleBackButton = false,
            childFactory = ::createChatOverlayComponent
        )

    private val _showBackArrow = MutableValue(false)
    override val showBackArrow: Value<Boolean> = _showBackArrow

    private val _shouldShowChatOverlay = MutableValue(false)
    override val shouldShowChatOverlay: Value<Boolean> = _shouldShowChatOverlay

    private var dashboardBackStateCancellation: Cancellation? = null
    private var dashboardPagesCancellation: Cancellation? = null

    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = childStackNavigation,
        stack = childStack,
        serializer = AppShellConfiguration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
        childSelector = { child ->
            (child.instance as? AppShellChildComponent.Dashboard)?.component
        }
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            appContainer.sessionManager.authenticationState.collect { state ->
                if (state !is AuthenticationState.LoggedIn) {
                    chatOverlayNavigation.dismiss()
                } else if (layoutMode == LayoutMode.MultiPane) {
                    // Restore overlay on login if desktop
                    chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay())
                }
            }
        }

        // Subscribe to stack changes to manage Back Arrow and Overlay Visibility
        childStack.subscribe { stack ->
            updateBackArrowState(stack)
            updateOverlayVisibility(stack)
        }
    }

    private fun updateBackArrowState(stack: ChildStack<AppShellConfiguration, AppShellChildComponent>) {
        dashboardBackStateCancellation?.cancel()
        dashboardBackStateCancellation = null

        if (stack.backStack.isNotEmpty()) {
            _showBackArrow.value = true
        } else {
            val activeInstance = stack.active.instance
            if (activeInstance is AppShellChildComponent.Dashboard) {
                dashboardBackStateCancellation = activeInstance.component.canNavigateBack.subscribe { canGoBack ->
                    _showBackArrow.value = canGoBack
                }
            } else {
                _showBackArrow.value = false
            }
        }
    }

    private fun updateOverlayVisibility(stack: ChildStack<AppShellConfiguration, AppShellChildComponent>) {
        dashboardPagesCancellation?.cancel()
        dashboardPagesCancellation = null

        // 1. Must be Desktop/MultiPane mode
        if (layoutMode != LayoutMode.MultiPane) {
            _shouldShowChatOverlay.value = false
            return
        }

        // 2. Must be on Dashboard (not separate Listing Editor or Chat route)
        val activeInstance = stack.active.instance
        if (activeInstance !is AppShellChildComponent.Dashboard) {
            _shouldShowChatOverlay.value = true
            // If we are in standalone Chat route (AppShellConfiguration.Chat), hide.
            if (stack.active.configuration is AppShellConfiguration.Chat) {
                _shouldShowChatOverlay.value = false
            }
            return
        }

        // 3. If on Dashboard, check the active Tab
        dashboardPagesCancellation = activeInstance.component.pages.subscribe { pages ->
            val activePage = pages.items[pages.selectedIndex]
            val isChatTab = activePage.configuration is DashboardConfiguration.Chat

            // Hide overlay if we are looking at the full-screen Chat Tab
            _shouldShowChatOverlay.value = !isChatTab
        }
    }

    override fun onLayoutModeChanged(mode: LayoutMode) {
        if (layoutMode == mode) return
        layoutMode = mode

        updateOverlayVisibility(childStack.value)

        if (mode == LayoutMode.SinglePane) {
            val overlayChild = chatOverlay.value.child
            if (overlayChild != null) {
                chatOverlayNavigation.dismiss()
            }
        } else {
            val activeConfiguration = childStack.value.active.configuration
            if (activeConfiguration is AppShellConfiguration.Chat) {
                childStackNavigation.pop()
                // FIX: Map AppShellConfiguration.Chat -> AppShellConfiguration.ChatOverlay(jid)
                chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay(activeConfiguration.recipientJid))
            } else if (authenticationState.value is AuthenticationState.LoggedIn) {
                chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay())
            }
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun expandChatOverlay() {
        // "Maximize" logic: Go to Dashboard Chat Tab
        val dashboard = childStack.value.items
            .map { it.instance }
            .filterIsInstance<AppShellChildComponent.Dashboard>()
            .firstOrNull()?.component

        if (dashboard != null) {
            dashboard.onChatTabClicked()
            if (childStack.value.active.configuration !is AppShellConfiguration.Dashboard) {
                childStackNavigation.popTo(0)
            }
        }
    }

    override fun dismissChatOverlay() {
        chatOverlayNavigation.dismiss()
    }

    override fun onBackClicked() {
        if (childStack.value.backStack.isNotEmpty()) {
            childStackNavigation.pop()
        } else {
            val activeInstance = childStack.value.active.instance
            if (activeInstance is AppShellChildComponent.Dashboard) {
                activeInstance.component.onBackClicked()
            }
        }
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
        val activeInstance = childStack.value.active.instance
        if (activeInstance is AppShellChildComponent.Dashboard) {
            activeInstance.component.onSettingsClicked()
        }
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

            else -> listOf(AppShellConfiguration.Dashboard)
        }
    }

    private fun calculateInitialOverlay(intent: AppShellIntent): AppShellConfiguration.ChatOverlay? {
        return null
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
                    messagingRepository = appContainer.messagingRepository,
                    inAppBrowserLauncher = appContainer.inAppBrowserLauncher,
                    onNavigateToListingDetails = { listingId ->
                    },
                    onNavigateToCreateListing = {
                        childStackNavigation.pushToFront(AppShellConfiguration.ListingEditor(null))
                    },
                    onNavigateToChat = { userId ->
                    },
                    onNavigateToChatByJid = { jid ->
                        if (layoutMode == LayoutMode.MultiPane) {
                            // Desktop: Open/Select in Overlay
                            // Ensure overlay is active
                            chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay(jid))

                            val overlay = chatOverlay.value.child?.instance
                            overlay?.component?.selectConversation(jid)
                        } else {
                            // Mobile: Switch Dashboard tab to Chat
                            val dashboard = childStack.value.items
                                .map { it.instance }
                                .filterIsInstance<AppShellChildComponent.Dashboard>()
                                .firstOrNull()?.component

                            dashboard?.selectChat(jid)
                        }
                    },
                    onNavigateToSettings = {
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

            is AppShellConfiguration.ChatOverlay -> {
                // Should not happen in main stack, but good to handle
                AppShellChildComponent.ChatOverlay(
                    ChatRootComponent(
                        componentContext = componentContext,
                        messagingRepository = appContainer.messagingRepository,
                        sessionManager = appContainer.sessionManager,
                        initialJid = configuration.initialJid,
                        onNavigateBack = { childStackNavigation.pop() }
                    )
                )
            }
        }
    }

    private fun createChatOverlayComponent(
        configuration: AppShellConfiguration.ChatOverlay,
        componentContext: ComponentContext
    ): AppShellChildComponent.ChatOverlay {
        return AppShellChildComponent.ChatOverlay(
            ChatRootComponent(
                componentContext = componentContext,
                messagingRepository = appContainer.messagingRepository,
                sessionManager = appContainer.sessionManager,
                initialJid = configuration.initialJid,
                onNavigateBack = {
                    // No-op for overlay back
                }
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

    data class ChatOverlay(val component: IChatRootComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())
    }
}