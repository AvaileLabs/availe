package com.availelabs.availe.appshell.component

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
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.appshell.router.AppShellRouter
import com.availelabs.availe.appshell.strategy.AppShellNavigationStrategy
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.chat.details.ChatComponent
import com.availelabs.availe.chat.root.ChatRootComponent
import com.availelabs.availe.chat.root.IChatNavigator
import com.availelabs.availe.dashboard.DashboardConfiguration
import com.availelabs.availe.decompose.asValue
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.home.HomeComponent
import com.availelabs.availe.home.IHomeNavigator
import com.availelabs.availe.homenav.HomeNavComponent
import com.availelabs.availe.listingdetails.IListingDetailsNavigator
import com.availelabs.availe.listingdetails.ListingDetailsComponent
import com.availelabs.availe.listingeditor.IListingEditorNavigator
import com.availelabs.availe.login.ILoginNavigator
import com.availelabs.availe.profile.IProfileNavigator
import com.availelabs.availe.profile.ProfileComponent
import com.availelabs.availe.profilenav.ProfileNavComponent
import com.availelabs.availe.settings.ISettingsNavigator
import com.availelabs.availe.settings.SettingsComponent
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
    fun onHomeTabClicked()
    fun onChatTabClicked()
    fun onProfileTabClicked()
}

@OptIn(ExperimentalUuidApi::class)
class AppShellComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val initialIntent: AppShellIntent,
    override val backDispatcher: BackDispatcher?,
    override val isAuthenticationLoading: Value<Boolean>,
    onExplicitLogin: () -> Unit,
    private val onCancelLogin: () -> Unit,
    private val onShowLoginGuard: () -> Unit
) : IAppShellComponent,
    ComponentContext by componentContext,
    IHomeNavigator,
    IChatNavigator,
    IListingEditorNavigator,
    IListingDetailsNavigator,
    IProfileNavigator,
    ISettingsNavigator,
    ILoginNavigator {

    private val childStackNavigation = StackNavigation<AppShellConfiguration>()
    private val chatOverlayNavigation = SlotNavigation<AppShellConfiguration.ChatOverlay>()
    private var layoutMode = LayoutMode.SinglePane

    private val router = AppShellRouter(
        stackNavigation = childStackNavigation,
        overlayNavigation = chatOverlayNavigation,
        layoutModeProvider = { layoutMode },
        inAppBrowserLauncher = appContainer.inAppBrowserLauncher,
        onExplicitLoginCallback = onExplicitLogin,
        onCancelLoginCallback = onCancelLogin,
        childStackProvider = { childStack }
    )

    private val _childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>> = childStack(
        source = childStackNavigation,
        serializer = AppShellConfiguration.serializer(),
        initialStack = { AppShellNavigationStrategy.calculateInitialStack(initialIntent) },
        handleBackButton = true,
        childFactory = ::createChildComponent
    )

    override val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>> = _childStack

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val authenticationState: Value<AuthenticationState> =
        appContainer.sessionManager.authenticationState.asValue(lifecycle)

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
            when (val instance = child.instance) {
                is AppShellChildComponent.Dashboard -> instance.component
                is AppShellChildComponent.Chat -> instance.component
                is AppShellChildComponent.Home -> instance.component
                else -> null
            }
        }
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }

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

        if (layoutMode != LayoutMode.MultiPane) {
            _shouldShowChatOverlay.value = false
            return
        }

        val activeInstance = stack.active.instance
        if (activeInstance !is AppShellChildComponent.Dashboard) {
            _shouldShowChatOverlay.value = true
            if (stack.active.configuration is AppShellConfiguration.Chat ||
                stack.active.configuration is AppShellConfiguration.ListingEditor
            ) {
                _shouldShowChatOverlay.value = false
            }
            return
        }

        dashboardPagesCancellation = activeInstance.component.pages.subscribe { pages ->
            val activePage = pages.items[pages.selectedIndex]
            val isChatTab = activePage.configuration is DashboardConfiguration.Chat
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
            if (activeConfiguration is AppShellConfiguration.Chat && activeConfiguration.recipientJid != null) {
                childStackNavigation.pop()
                chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay(activeConfiguration.recipientJid))
            } else {
                chatOverlayNavigation.activate(AppShellConfiguration.ChatOverlay())
            }
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun expandChatOverlay() {
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
        router.dismissChatOverlay()
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
        router.onLoginClicked()
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
        router.onSettingsClicked()
    }

    override fun onHomeTabClicked() {
        router.navigateToHome()
    }

    override fun onChatTabClicked() {
        scope.launch {
            val userProfile = appContainer.sessionManager.getCurrentUserProfile()
            if (userProfile != null) {
                router.navigateToChat(userProfile.jid)
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun onProfileTabClicked() {
        scope.launch {
            val userProfile = appContainer.sessionManager.getCurrentUserProfile()
            if (userProfile != null) {
                router.navigateToProfile(userProfile.id)
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun navigateToListingDetails(listingId: ListingId) = router.navigateToListingDetails(listingId)
    override fun navigateToCreateListing(category: ListingCategory?) = router.navigateToCreateListing(category)
    override fun navigateBack() = router.navigateBack()
    override fun navigateToChat(jid: Jid) {
        router.navigateToChat(jid)
        if (layoutMode == LayoutMode.MultiPane) {
            val overlay = chatOverlay.value.child?.instance
            overlay?.component?.selectConversation(jid)
        }
    }

    override fun navigateToProfile(userId: UserId) = router.navigateToProfile(userId)
    override fun navigateToListing(listingId: ListingId) = router.navigateToListing(listingId)
    override fun navigateToChat(userId: UserId) = router.navigateToChat(userId)
    override fun navigateToSettings() = router.navigateToSettings()
    override fun openUrl(url: String) = router.openUrl(url)
    override fun dismissLogin() = router.dismissLogin()
    override fun navigateToEditListing(listingId: ListingId) = router.navigateToEditListing(listingId)

    private fun createChildComponent(
        configuration: AppShellConfiguration,
        componentContext: ComponentContext
    ): AppShellChildComponent {
        val context = AppShellChildFactoryContext(
            componentContext = componentContext,
            appContainer = appContainer,
            navigator = router,
            initialIntent = initialIntent,
            onShowLoginGuard = onShowLoginGuard,
            homeNavFactory = { context, initialStack ->
                HomeNavComponent(
                    componentContext = context,
                    initialStack = initialStack,
                    homeFactory = { childContext, feedType ->
                        HomeComponent(
                            componentContext = childContext,
                            listingRepository = appContainer.listingRepository,
                            sessionManager = appContainer.sessionManager,
                            messageService = appContainer.messageService,
                            navigator = router,
                            initialFeedType = feedType
                        )
                    },
                    listingDetailsFactory = { childContext, listingId ->
                        ListingDetailsComponent(
                            componentContext = childContext,
                            listingId = listingId,
                            listingRepository = appContainer.listingRepository,
                            sessionManager = appContainer.sessionManager,
                            interactionRepository = appContainer.interactionRepository,
                            messageService = appContainer.messageService,
                            navigator = router
                        )
                    },
                    profileFactory = { childContext, userId ->
                        ProfileComponent(
                            componentContext = childContext,
                            targetUserId = userId,
                            listingRepository = appContainer.listingRepository,
                            userRepository = appContainer.userRepository,
                            sessionManager = appContainer.sessionManager,
                            messageService = appContainer.messageService,
                            navigator = router
                        )
                    }
                )
            },
            chatRootFactory = { context, jid ->
                ChatRootComponent(
                    componentContext = context,
                    messagingRepository = appContainer.messagingRepository,
                    sessionManager = appContainer.sessionManager,
                    initialJid = jid,
                    navigator = router
                )
            },
            profileNavFactory = { context, userId, initialStack ->
                ProfileNavComponent(
                    componentContext = context,
                    userId = userId,
                    initialStack = initialStack,
                    profileFactory = { childContext, targetUserId ->
                        ProfileComponent(
                            componentContext = childContext,
                            targetUserId = targetUserId,
                            listingRepository = appContainer.listingRepository,
                            userRepository = appContainer.userRepository,
                            sessionManager = appContainer.sessionManager,
                            messageService = appContainer.messageService,
                            navigator = router
                        )
                    },
                    listingDetailsFactory = { childContext, listingId ->
                        ListingDetailsComponent(
                            componentContext = childContext,
                            listingId = listingId,
                            listingRepository = appContainer.listingRepository,
                            sessionManager = appContainer.sessionManager,
                            interactionRepository = appContainer.interactionRepository,
                            messageService = appContainer.messageService,
                            navigator = router
                        )
                    },
                    settingsFactory = { childContext ->
                        SettingsComponent(
                            componentContext = childContext,
                            storeFactory = DefaultStoreFactory(),
                            sessionManager = appContainer.sessionManager,
                            authenticationRepository = appContainer.authenticationRepository,
                            userRepository = appContainer.userRepository,
                            navigator = router
                        )
                    }
                )
            },
            homeFactory = { context, feedType ->
                HomeComponent(
                    componentContext = context,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    messageService = appContainer.messageService,
                    navigator = router,
                    initialFeedType = feedType
                )
            },
            listingDetailsFactory = { context, listingId ->
                ListingDetailsComponent(
                    componentContext = context,
                    listingId = listingId,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    interactionRepository = appContainer.interactionRepository,
                    messageService = appContainer.messageService,
                    navigator = router
                )
            },
            profileFactory = { context, userId ->
                ProfileComponent(
                    componentContext = context,
                    targetUserId = userId,
                    listingRepository = appContainer.listingRepository,
                    userRepository = appContainer.userRepository,
                    sessionManager = appContainer.sessionManager,
                    messageService = appContainer.messageService,
                    navigator = router
                )
            },
            settingsFactory = { context ->
                SettingsComponent(
                    componentContext = context,
                    storeFactory = DefaultStoreFactory(),
                    sessionManager = appContainer.sessionManager,
                    authenticationRepository = appContainer.authenticationRepository,
                    userRepository = appContainer.userRepository,
                    navigator = router
                )
            },
            chatFactory = { context, jid ->
                ChatComponent(
                    componentContext = context,
                    recipientJid = jid,
                    messagingRepository = appContainer.messagingRepository,
                    sessionManager = appContainer.sessionManager,
                    onNavigateBack = { router.navigateBack() }
                )
            }
        )

        return AppShellNavigationStrategy.createChild(configuration, context)
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
                navigator = router
            )
        )
    }
}