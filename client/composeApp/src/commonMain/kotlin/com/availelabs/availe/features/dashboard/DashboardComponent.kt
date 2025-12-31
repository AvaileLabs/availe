package com.availelabs.availe.features.dashboard

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.features.appshell.AppShellIntent
import com.availelabs.availe.features.homenav.HomeNavComponent
import com.availelabs.availe.features.homenav.HomeNavConfiguration
import com.availelabs.availe.features.homenav.IHomeNavComponent
import com.availelabs.availe.features.profilenav.IProfileNavComponent
import com.availelabs.availe.features.profilenav.ProfileNavComponent
import com.availelabs.availe.features.profilenav.ProfileNavConfiguration
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*

interface IDashboardComponent {
    val pages: Value<ChildPages<DashboardConfiguration, DashboardChildComponent>>
    val actions: Value<List<AppBarAction>>
    val canNavigateBack: Value<Boolean>

    fun onHomeTabClicked()
    fun onProfileTabClicked()
    fun onPageSelected(index: Int)
    fun onBackClicked()
}

sealed interface DashboardChildComponent {
    data class HomeNav(val component: IHomeNavComponent) : DashboardChildComponent
    data class ProfileNav(val component: IProfileNavComponent) : DashboardChildComponent
}

class DashboardComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val authenticationRepository: IAuthenticationRepository,
    private val messageService: IMessageService,
    private val inAppBrowserLauncher: IInAppBrowserLauncher,
    private val onNavigateToListingDetails: (ListingId) -> Unit,
    private val onNavigateToCreateListing: () -> Unit,
    private val onNavigateToChat: (UserId) -> Unit,
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

    init {
        pages.subscribe { childPages ->
            val selectedIndex = childPages.selectedIndex
            val activeChildInstance = childPages.items[selectedIndex].instance

            currentChildActionsCancellation?.cancel()
            currentChildBackStateCancellation?.cancel()

            // Update Actions
            val newChildActionsValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.actions
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.actions
                null -> MutableValue(emptyList())
            }
            currentChildActionsCancellation = newChildActionsValue.subscribe { newActions ->
                _actions.value = newActions
            }

            // Update Back State
            val newChildBackStateValue = when (activeChildInstance) {
                is DashboardChildComponent.HomeNav -> activeChildInstance.component.canNavigateBack
                is DashboardChildComponent.ProfileNav -> activeChildInstance.component.canNavigateBack
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
            // If already on home tab, reset the stack
            val homeInstance = pages.value.items[0].instance as? DashboardChildComponent.HomeNav
            homeInstance?.component?.reset()
        } else {
            navigation.select(0)
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
                            DashboardConfiguration.Profile(userProfile.id)
                        )
                    )
                }

                // If we are already on the profile tab (index 1), reset it
                val currentIndex = pages.value.selectedIndex
                if (currentIndex == 1) {
                    val profileInstance = pages.value.items[1].instance as? DashboardChildComponent.ProfileNav
                    profileInstance?.component?.reset()
                } else {
                    navigation.select(1)
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
        val activeInstance = pages.value.items[selectedIndex].instance

        when (activeInstance) {
            is DashboardChildComponent.HomeNav -> activeInstance.component.onBackClicked()
            is DashboardChildComponent.ProfileNav -> activeInstance.component.onBackClicked()
            null -> {}
        }
    }

    private fun createChild(
        configuration: DashboardConfiguration,
        componentContext: ComponentContext
    ): DashboardChildComponent {
        return when (configuration) {
            is DashboardConfiguration.Home -> {
                // Determine initial stack based on intent only if it's relevant to Home
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
                        onNavigateToSettings = onNavigateToSettings,
                        initialStack = initialStack
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
                        onOpenUrl = { url -> inAppBrowserLauncher.open(url) },
                        initialStack = initialStack
                    )
                )
            }
        }
    }

    private fun calculateInitialPages(intent: AppShellIntent?): Pages<DashboardConfiguration> {
        // Basic logic: Default to Home.
        return Pages(
            items = listOf(DashboardConfiguration.Home),
            selectedIndex = 0
        )
    }

    @OptIn(ExperimentalDecomposeApi::class)
    fun updatePages(configurations: List<DashboardConfiguration>) {
        navigation.setItems { configurations }
    }
}