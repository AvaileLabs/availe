package com.availelabs.availe.features.homenav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listingdetails.IListingDetailsComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsComponent
import com.availelabs.availe.features.profile.IProfileComponent
import com.availelabs.availe.features.profile.ProfileComponent
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.AppBarAction
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IHomeNavComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<HomeNavConfiguration, HomeNavChildComponent>>
    val actions: Value<List<AppBarAction>>
    val canNavigateBack: Value<Boolean>

    fun onBackClicked()
    fun reset()
}

sealed interface HomeNavChildComponent {
    data class HomeFeed(val component: IHomeComponent) : HomeNavChildComponent
    data class ListingDetails(val component: IListingDetailsComponent) : HomeNavChildComponent
    data class Profile(val component: IProfileComponent) : HomeNavChildComponent
}

class HomeNavComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val onNavigateToCreateListing: () -> Unit,
    private val onNavigateToChat: (UserId) -> Unit,
    private val onNavigateToChatByJid: (Jid) -> Unit,
    private val onNavigateToSettings: () -> Unit,
    private val initialStack: List<HomeNavConfiguration> = listOf(HomeNavConfiguration.HomeFeed)
) : IHomeNavComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<HomeNavConfiguration>()

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    override val childStack: Value<ChildStack<HomeNavConfiguration, HomeNavChildComponent>> = childStack(
        source = navigation,
        serializer = HomeNavConfiguration.serializer(),
        initialStack = { initialStack },
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val canNavigateBack: Value<Boolean> = childStack.map { it.backStack.isNotEmpty() }

    @OptIn(ExperimentalDecomposeApi::class, ExperimentalUuidApi::class)
    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = navigation,
        stack = childStack,
        serializer = HomeNavConfiguration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
        childSelector = { null }
    )

    init {
        childStack.subscribe { stack ->
            when (val activeComponent = stack.active.instance) {
                is HomeNavChildComponent.HomeFeed -> {
                    activeComponent.component.actions.subscribe { _actions.value = it }
                }

                is HomeNavChildComponent.ListingDetails -> {
                    activeComponent.component.actions.subscribe { _actions.value = it }
                }

                is HomeNavChildComponent.Profile -> {
                    activeComponent.component.actions.subscribe { _actions.value = it }
                }
            }
        }
    }

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun reset() {
        navigation.popToFirst()
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(
        configuration: HomeNavConfiguration,
        componentContext: ComponentContext
    ): HomeNavChildComponent {
        return when (configuration) {
            is HomeNavConfiguration.HomeFeed -> HomeNavChildComponent.HomeFeed(
                HomeComponent(
                    componentContext = componentContext,
                    listingRepository = listingRepository,
                    sessionManager = sessionManager,
                    messageService = messageService,
                    onNavigateToListingDetails = { listingId ->
                        navigation.push(HomeNavConfiguration.ListingDetails(listingId))
                    },
                    onNavigateToCreateListing = onNavigateToCreateListing
                )
            )

            is HomeNavConfiguration.ListingDetails -> HomeNavChildComponent.ListingDetails(
                ListingDetailsComponent(
                    componentContext = componentContext,
                    listingId = configuration.listingId,
                    listingRepository = listingRepository,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToChat = onNavigateToChatByJid,
                    onNavigateToProfile = { userId ->
                        navigation.bringToFront(HomeNavConfiguration.Profile(userId))
                    }
                )
            )

            is HomeNavConfiguration.Profile -> HomeNavChildComponent.Profile(
                ProfileComponent(
                    componentContext = componentContext,
                    targetUserId = configuration.userId,
                    listingRepository = listingRepository,
                    sessionManager = sessionManager,
                    messageService = messageService,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToListing = { listingId ->
                        navigation.push(HomeNavConfiguration.ListingDetails(listingId))
                    },
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToSettings = onNavigateToSettings
                )
            )
        }
    }
}