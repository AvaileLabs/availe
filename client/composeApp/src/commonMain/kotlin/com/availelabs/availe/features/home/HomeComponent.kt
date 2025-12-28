package com.availelabs.availe.features.home

import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.accessibility_action_add
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.features.listingfeed.IListingFeedComponent
import com.availelabs.availe.features.listingfeed.ListingFeedComponent
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.ActionIcon
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.*

interface IHomeComponent {
    val pages: Value<ChildPages<HomeConfiguration, IListingFeedComponent>>
    val actions: Value<List<AppBarAction>>
    fun onTabClicked(index: Int)
}

class HomeComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val onNavigateToListingDetails: (ListingId) -> Unit,
    private val onNavigateToCreateListing: () -> Unit,
    private val messageService: IMessageService
) : IHomeComponent, ComponentContext by componentContext {

    private val navigation = PagesNavigation<HomeConfiguration>()

    override val pages: Value<ChildPages<HomeConfiguration, IListingFeedComponent>> =
        childPages(
            source = navigation,
            serializer = HomeConfiguration.serializer(),
            initialPages = {
                Pages(
                    items = listOf(
                        HomeConfiguration.Feed(ListingCategory.Item),
                        HomeConfiguration.Feed(ListingCategory.Event),
                        HomeConfiguration.Feed(ListingCategory.Service)
                    ),
                    selectedIndex = 0
                )
            },
            childFactory = ::createChild
        )

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            sessionManager.authenticationState.collect { authenticationState ->
                if (authenticationState is AuthenticationState.LoggedIn) {
                    _actions.value = listOf(
                        AppBarAction(
                            icon = ActionIcon.ADD,
                            label = UiText.ResourceString(Res.string.accessibility_action_add),
                            onClick = onNavigateToCreateListing
                        )
                    )
                } else {
                    _actions.value = emptyList()
                }
            }
        }
    }

    override fun onTabClicked(index: Int) {
        navigation.select(index)
    }

    private fun createChild(
        configuration: HomeConfiguration,
        componentContext: ComponentContext
    ): IListingFeedComponent {
        return when (configuration) {
            is HomeConfiguration.Feed -> ListingFeedComponent(
                componentContext = componentContext,
                category = configuration.category,
                listingRepository = listingRepository,
                sessionManager = sessionManager,
                messageService = messageService,
                onNavigateToListingDetails = onNavigateToListingDetails
            )
        }
    }
}