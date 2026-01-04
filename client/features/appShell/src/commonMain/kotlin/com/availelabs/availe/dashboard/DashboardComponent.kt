package com.availelabs.availe.dashboard

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.appshell.AppShellIntent
import com.availelabs.availe.chat.root.ChatRootComponent
import com.availelabs.availe.chat.root.IChatRootComponent
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.homenav.HomeNavComponent
import com.availelabs.availe.homenav.HomeNavConfiguration
import com.availelabs.availe.homenav.IHomeNavComponent
import com.availelabs.availe.profilenav.IProfileNavComponent
import com.availelabs.availe.profilenav.ProfileNavComponent
import com.availelabs.availe.profilenav.ProfileNavConfiguration
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IDashboardComponent : WebNavigationOwner {
    val pages: Value<ChildPages<DashboardConfiguration, DashboardChildComponent>>
    val actions: Value<List<AppBarAction>>
    val canNavigateBack: Value<Boolean>

    fun onHomeTabClicked()
    fun onChatTabClicked()
    fun onProfileTabClicked()
    fun onPageSelected(index: Int)
    fun onBackClicked()
    fun onSettingsClicked()
    fun selectChat(jid: Jid)
}

sealed interface DashboardChildComponent {
    data class HomeNav(val component: IHomeNavComponent) : DashboardChildComponent
    data class ChatRoot(val component: IChatRootComponent) : DashboardChildComponent
    data class ProfileNav(val component: IProfileNavComponent) : DashboardChildComponent
}

class DashboardComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val authenticationRepository: IAuthenticationRepository,
    private val messageService: IMessageService,
    private val messagingRepository: IMessagingRepository,
    private val inAppBrowserLauncher: IInAppBrowserLauncher,
    private val onNavigateToListingDetails: (ListingId) -> Unit,
    private val onNavigateToCreateListing: () -> Unit,
    private val onNavigateToChat: (UserId) -> Unit,
    private val onNavigateToChatByJid: (Jid) -> Unit,
    private val onNavigateToSettings: () -> Unit,
    private val onShowLoginGuard: () -> Unit,
    private val initialIntent: AppShellIntent? = null
) : IDashboardComponent, ComponentContext by componentContext {

    private val navigation = PagesNavigation<DashboardConfiguration>()

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    private val _canNavigateBack = MutableValue(false)
    override val canNavigateBack: Value<Boolean> = _canNavigateBack

    private var currentChildActionsCancellation: Cancellation? = null
    private var currentChildBackStateCancellation: Cancellation? = null

    override val pages: Value<ChildPages<DashboardConfiguration, DashboardChildComponent>> =
        childPages(
            source = navigation,
            serializer = DashboardConfiguration.serializer(),
            initialPages = { calculateInitialPages(initialIntent) },
            childFactory = ::createChild
        )

    @OptIn(ExperimentalDecomposeApi::class, ExperimentalUuidApi::class)
    override val webNavigation: WebNavigation<*> = childPagesWebNavigation(
        navigator = navigation,
        pages = pages,
        serializer = DashboardConfiguration.serializer(),
        pathMapper = { childPages ->
            childPages.items[childPages.selectedIndex].configuration.toUrlPath()
        },
        childSelector = { child ->
            when (val instance = child.instance) {
                is DashboardChildComponent.HomeNav -> instance.component
                is DashboardChildComponent.ChatRoot -> null // Chat root navigation handled internally if needed
                is DashboardChildComponent.ProfileNav -> instance.component
            }
        }
    )

    init {
        pages.subscribe { childPages ->
            val selectedIndex = childPages.selectedIndex
            val activeChildInstance = childPages.items[selectedIndex].instance

            currentChildActionsCancellation?.cancel()
            currentChildBackStateCancellation?.cancel()

            // Update Actions based on active tab
            val newChildActionsValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.actions
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.actions
                is DashboardChildComponent.ChatRoot -> MutableValue(emptyList()) // Chat usually handles its own top bar or has none
                null -> MutableValue(emptyList())
            }
            currentChildActionsCancellation = newChildActionsValue.subscribe { newActions ->
                _actions.value = newActions
            }

            // Update Back Navigation capability
            val newChildBackStateValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.canNavigateBack
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.canNavigateBack
                is DashboardChildComponent.ChatRoot -> MutableValue(false)
                null -> MutableValue(false)
            }
            currentChildBackStateCancellation = newChildBackStateValue.subscribe { canGoBack ->
                _canNavigateBack.value = canGoBack
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onHomeTabClicked() {
        val currentIndex = pages.value.selectedIndex
        if (currentIndex == 0) {
            val homeInstance = pages.value.items[0].instance as? DashboardChildComponent.HomeNav
            homeInstance?.component?.reset()
        } else {
            navigation.select(0)
        }
    }

    override fun onChatTabClicked() {
        scope.launch {
            val userProfile = sessionManager.getCurrentUserProfile()
            if (userProfile != null) {
                // Ensure Chat configuration exists (it might be duplicated if we push, but Pages usually unique)
                // We use replace or select.
                // Assuming standard 3 tabs setup: Home(0), Chat(1), Profile(2)
                navigation.select(1)
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun onProfileTabClicked() {
        scope.launch {
            val userProfile = sessionManager.getCurrentUserProfile()
            if (userProfile != null) {
                // Check if profile is at index 2, if so select it.
                // Dynamic page updating is complex, we assume static 3 tabs for now based on createChild logic
                // except initial load can be different.
                // To be safe with `updatePages` we ensure structure.
                if (pages.value.items.none { it.configuration is DashboardConfiguration.Profile }) {
                    updatePages(
                        listOf(
                            DashboardConfiguration.Home,
                            DashboardConfiguration.Chat(),
                            DashboardConfiguration.Profile(userProfile.id)
                        )
                    )
                }

                val profileIndex = pages.value.items.indexOfFirst { it.configuration is DashboardConfiguration.Profile }
                val currentIndex = pages.value.selectedIndex

                if (currentIndex == profileIndex && profileIndex != -1) {
                    val profileInstance =
                        pages.value.items[profileIndex].instance as? DashboardChildComponent.ProfileNav
                    profileInstance?.component?.reset()
                } else if (profileIndex != -1) {
                    navigation.select(profileIndex)
                }
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun onPageSelected(index: Int) {
        navigation.select(index)
    }

    override fun onBackClicked() {
        val selectedIndex = pages.value.selectedIndex
        when (val activeInstance = pages.value.items[selectedIndex].instance) {
            is DashboardChildComponent.HomeNav -> activeInstance.component.onBackClicked()
            is DashboardChildComponent.ProfileNav -> activeInstance.component.onBackClicked()
            is DashboardChildComponent.ChatRoot -> {} // Chat root typically doesn't have a back stack in this context
            null -> {}
        }
    }

    override fun onSettingsClicked() {
        scope.launch {
            val userProfile = sessionManager.getCurrentUserProfile()
            if (userProfile != null) {
                if (pages.value.items.none { it.configuration is DashboardConfiguration.Profile }) {
                    updatePages(
                        listOf(
                            DashboardConfiguration.Home,
                            DashboardConfiguration.Chat(),
                            DashboardConfiguration.Profile(userProfile.id)
                        )
                    )
                }

                val profileIndex = pages.value.items.indexOfFirst { it.configuration is DashboardConfiguration.Profile }
                if (profileIndex != -1) {
                    navigation.select(profileIndex)
                    val profileItem = pages.value.items[profileIndex]
                    val instance = profileItem.instance as? DashboardChildComponent.ProfileNav
                    instance?.component?.navigateToSettings()
                }
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun selectChat(jid: Jid) {
        // Switch to Chat tab
        val chatIndex = pages.value.items.indexOfFirst { it.configuration is DashboardConfiguration.Chat }
        if (chatIndex != -1) {
            navigation.select(chatIndex)
            // If the component is already created, we need to tell it to select the Jid.
            // Since `pages` is observable, we can try to get the instance immediately if active,
            // or we rely on the configuration update.
            // However, Pages navigation doesn't easily support updating the configuration of an existing page
            // to pass data without recreating it or using a side channel.
            // Best approach here: Cast the component instance and call a method.
            val chatInstance = pages.value.items[chatIndex].instance as? DashboardChildComponent.ChatRoot
            chatInstance?.component?.selectConversation(jid)
        }
    }

    private fun createChild(
        configuration: DashboardConfiguration,
        componentContext: ComponentContext
    ): DashboardChildComponent {
        return when (configuration) {
            is DashboardConfiguration.Home -> {
                val initialStack = when (initialIntent) {
                    is AppShellIntent.ViewingListing -> listOf(
                        HomeNavConfiguration.HomeFeed,
                        HomeNavConfiguration.ListingDetails(initialIntent.listingId)
                    )

                    is AppShellIntent.ViewingListingWithChat -> listOf(
                        HomeNavConfiguration.HomeFeed,
                        HomeNavConfiguration.ListingDetails(initialIntent.listingId)
                    )

                    is AppShellIntent.ViewingProfile -> listOf(
                        HomeNavConfiguration.HomeFeed,
                        HomeNavConfiguration.Profile(initialIntent.userId)
                    )

                    else -> listOf(HomeNavConfiguration.HomeFeed)
                }

                DashboardChildComponent.HomeNav(
                    HomeNavComponent(
                        componentContext = componentContext,
                        listingRepository = listingRepository,
                        sessionManager = sessionManager,
                        messageService = messageService,
                        onNavigateToCreateListing = onNavigateToCreateListing,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToChatByJid = onNavigateToChatByJid, // Passed from AppShell
                        onNavigateToSettings = onNavigateToSettings,
                        initialStack = initialStack
                    )
                )
            }

            is DashboardConfiguration.Chat -> {
                DashboardChildComponent.ChatRoot(
                    ChatRootComponent(
                        componentContext = componentContext,
                        messagingRepository = messagingRepository,
                        sessionManager = sessionManager,
                        initialJid = configuration.initialJid,
                        onNavigateBack = { /* Chat root back handling */ }
                    )
                )
            }

            is DashboardConfiguration.Profile -> {
                val initialStack = if (initialIntent is AppShellIntent.Settings) {
                    listOf(
                        ProfileNavConfiguration.Profile(configuration.userId),
                        ProfileNavConfiguration.Settings
                    )
                } else {
                    listOf(ProfileNavConfiguration.Profile(configuration.userId))
                }

                DashboardChildComponent.ProfileNav(
                    ProfileNavComponent(
                        componentContext = componentContext,
                        userId = configuration.userId,
                        listingRepository = listingRepository,
                        sessionManager = sessionManager,
                        authenticationRepository = authenticationRepository,
                        messageService = messageService,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToChatByJid = onNavigateToChatByJid,
                        onOpenUrl = { url -> inAppBrowserLauncher.open(url) },
                        initialStack = initialStack
                    )
                )
            }
        }
    }

    private fun calculateInitialPages(intent: AppShellIntent?): Pages<DashboardConfiguration> {
        // Initialize with all 3 tabs to ensure index alignment
        // We can't know the userId for Profile yet if not logged in or passed,
        // so we might need a default list or dynamic update on login.
        // For simplicity, we start with Home and Chat. Profile is added dynamically on login in existing logic.
        // Let's add Chat by default.
        return Pages(
            items = listOf(
                DashboardConfiguration.Home,
                DashboardConfiguration.Chat()
            ),
            selectedIndex = 0
        )
    }

    @OptIn(ExperimentalDecomposeApi::class)
    fun updatePages(configurations: List<DashboardConfiguration>) {
        navigation.setItems { configurations }
    }
}