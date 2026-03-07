package com.availelabs.availe.home

import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.accessibility_action_add
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.listingfeed.IListingFeedComponent
import com.availelabs.availe.listingfeed.ListingFeedComponent
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.model.toEventFeedFilter
import com.availelabs.availe.model.toListingCategoryFilter
import com.availelabs.availe.navigation.FeedTypeMapper
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.ActionIcon
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.*

@OptIn(ExperimentalDecomposeApi::class)
interface IHomeComponent : WebNavigationOwner {
    val pages: Value<ChildPages<HomeConfiguration, IListingFeedComponent>>
    val actions: Value<List<AppBarAction>>
    fun onTabClicked(index: Int)
}

class HomeComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val navigator: IHomeNavigator,
    private val initialFeedType: ListingFeedType = ListingFeedType.Upcoming
) : IHomeComponent, ComponentContext by componentContext {

    private val navigation = PagesNavigation<HomeConfiguration>()

    override val pages: Value<ChildPages<HomeConfiguration, IListingFeedComponent>> =
        childPages(
            source = navigation,
            serializer = HomeConfiguration.serializer(),
            initialPages = {
                val feedTypes = listOf(
                    ListingFeedType.Upcoming,
                    ListingFeedType.Going,
                    ListingFeedType.Past
                )

                val initialIndex = feedTypes.indexOf(initialFeedType).coerceAtLeast(0)

                Pages(
                    items = feedTypes.map { HomeConfiguration.Feed(it) },
                    selectedIndex = initialIndex
                )
            },
            childFactory = ::createChild
        )

    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> = childPagesWebNavigation(
        navigator = navigation,
        pages = pages,
        serializer = HomeConfiguration.serializer(),
        pathMapper = { pages ->
            val selectedChild = pages.items[pages.selectedIndex]
            val configuration = selectedChild.configuration as HomeConfiguration.Feed
            FeedTypeMapper.toPathSegment(configuration.feedType)
        }
    )

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        runStructuredPrefetching(scope)

        scope.launch {
            sessionManager.authenticationState.collect { authenticationState ->
                if (authenticationState is AuthenticationState.LoggedIn) {
                    _actions.value = listOf(
                        AppBarAction(
                            icon = ActionIcon.ADD,
                            label = UiText.ResourceString(Res.string.accessibility_action_add),
                            onClick = { navigator.navigateToCreateListing(ListingCategory.Event) }
                        )
                    )
                } else {
                    _actions.value = emptyList()
                }
            }
        }
    }

    private fun runStructuredPrefetching(scope: CoroutineScope) {
        val allFeedTypes = listOf(
            ListingFeedType.Upcoming,
            ListingFeedType.Going,
            ListingFeedType.Past
        )

        scope.launch(Dispatchers.Default) {
            for (feedType in allFeedTypes) {
                val currentPages = pages.value
                val activeIndex = currentPages.selectedIndex
                val activeConfiguration = currentPages.items[activeIndex].configuration

                if (activeConfiguration is HomeConfiguration.Feed && activeConfiguration.feedType == feedType) {
                    continue
                }

                try {
                    val filter = feedType.toListingCategoryFilter()
                    val eventFeedFilter = feedType.toEventFeedFilter()
                    if (listingRepository.isListingCacheStale(filter, eventFeedFilter)) {
                        listingRepository.prefetchListings(
                            page = 0,
                            size = 20,
                            filter = filter,
                            eventFeedFilter = eventFeedFilter
                        )
                    }
                } catch (_: Exception) {

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
                feedType = configuration.feedType,
                listingRepository = listingRepository,
                sessionManager = sessionManager,
                messageService = messageService,
                onNavigateToListingDetails = navigator::navigateToListingDetails
            )
        }
    }
}