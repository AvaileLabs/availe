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
import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.chat.root.IChatRootComponent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.homenav.HomeNavConfiguration
import com.availelabs.availe.homenav.IHomeNavComponent
import com.availelabs.availe.profilenav.IProfileNavComponent
import com.availelabs.availe.profilenav.ProfileNavConfiguration
import com.availelabs.availe.repositories.ISessionRepository
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
    private val sessionManager: ISessionRepository,
    private val homeNavFactory: (ComponentContext, List<HomeNavConfiguration>) -> IHomeNavComponent,
    private val chatRootFactory: (ComponentContext, Jid?) -> IChatRootComponent,
    private val profileNavFactory: (ComponentContext, UserId, List<ProfileNavConfiguration>) -> IProfileNavComponent,
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
                is DashboardChildComponent.ChatRoot -> null
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

            val newChildActionsValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.actions
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.actions
                is DashboardChildComponent.ChatRoot -> MutableValue(emptyList())
                null -> MutableValue(emptyList())
            }
            currentChildActionsCancellation = newChildActionsValue.subscribe { newActions ->
                _actions.value = newActions
            }

            val newChildBackStateValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.canNavigateBack
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.canNavigateBack
                is DashboardChildComponent.ChatRoot -> activeChildInstance.component.canNavigateBack
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
                val currentIndex = pages.value.selectedIndex
                val chatIndex = pages.value.items.indexOfFirst { it.configuration is DashboardConfiguration.Chat }

                if (currentIndex == chatIndex && chatIndex != -1) {
                    val chatInstance = pages.value.items[chatIndex].instance as? DashboardChildComponent.ChatRoot
                    chatInstance?.component?.reset()
                } else if (chatIndex != -1) {
                    navigation.select(chatIndex)
                }
            } else {
                onShowLoginGuard()
            }
        }
    }

    override fun onProfileTabClicked() {
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
            is DashboardChildComponent.ChatRoot -> {
                if (activeInstance.component.canNavigateBack.value) {
                    activeInstance.component.onBackClicked()
                }
            }

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
        val chatIndex = pages.value.items.indexOfFirst { it.configuration is DashboardConfiguration.Chat }
        if (chatIndex != -1) {
            navigation.select(chatIndex)
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
                    homeNavFactory(componentContext, initialStack)
                )
            }

            is DashboardConfiguration.Chat -> {
                DashboardChildComponent.ChatRoot(
                    chatRootFactory(componentContext, configuration.initialJid)
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
                    profileNavFactory(componentContext, configuration.userId, initialStack)
                )
            }
        }
    }

    private fun calculateInitialPages(intent: AppShellIntent?): Pages<DashboardConfiguration> {
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