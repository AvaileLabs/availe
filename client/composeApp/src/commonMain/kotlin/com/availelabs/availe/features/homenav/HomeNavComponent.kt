package com.availelabs.availe.features.homenav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
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

interface IHomeNavComponent : BackHandlerOwner {
    val childStack: Value<ChildStack<HomeNavConfiguration, HomeNavChildComponent>>
    val actions: Value<List<AppBarAction>>
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

    init {
        childStack.subscribe { stack ->
            val activeComponent = stack.active.instance
            when (activeComponent) {
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
                    onNavigateToChat = { jid ->
                        // Placeholder logic
                    },
                    onNavigateToProfile = { userId ->
                        navigation.push(HomeNavConfiguration.Profile(userId))
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