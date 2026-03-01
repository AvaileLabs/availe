package com.availelabs.availe.homenav

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
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.home.IHomeComponent
import com.availelabs.availe.listingdetails.IListingDetailsComponent
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.profile.IProfileComponent
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
    private val homeFactory: (ComponentContext, ListingFeedType) -> IHomeComponent,
    private val listingDetailsFactory: (ComponentContext, ListingId) -> IListingDetailsComponent,
    private val profileFactory: (ComponentContext, UserId) -> IProfileComponent,
    private val initialStack: List<HomeNavConfiguration>
) : IHomeNavComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        homeFactory: (ComponentContext, ListingFeedType) -> IHomeComponent,
        listingDetailsFactory: (ComponentContext, ListingId) -> IListingDetailsComponent,
        profileFactory: (ComponentContext, UserId) -> IProfileComponent
    ) : this(
        componentContext,
        homeFactory,
        listingDetailsFactory,
        profileFactory,
        listOf(HomeNavConfiguration.HomeFeed(ListingFeedType.Upcoming))
    )

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
                homeFactory(componentContext, configuration.feedType)
            )

            is HomeNavConfiguration.ListingDetails -> HomeNavChildComponent.ListingDetails(
                listingDetailsFactory(componentContext, configuration.listingId)
            )

            is HomeNavConfiguration.Profile -> HomeNavChildComponent.Profile(
                profileFactory(componentContext, configuration.userId)
            )
        }
    }
}