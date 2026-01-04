package com.availelabs.availe.profilenav

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
import com.availelabs.availe.listingdetails.IListingDetailsComponent
import com.availelabs.availe.profile.IProfileComponent
import com.availelabs.availe.settings.ISettingsComponent
import com.availelabs.availe.ui.model.AppBarAction
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalDecomposeApi::class)
interface IProfileNavComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<ProfileNavConfiguration, ProfileNavChildComponent>>
    val actions: Value<List<AppBarAction>>
    val canNavigateBack: Value<Boolean>

    fun onBackClicked()
    fun reset()
    fun navigateToSettings()
}

sealed interface ProfileNavChildComponent {
    data class Profile(val component: IProfileComponent) : ProfileNavChildComponent
    data class ListingDetails(val component: IListingDetailsComponent) : ProfileNavChildComponent
    data class Settings(val component: ISettingsComponent) : ProfileNavChildComponent
}

class ProfileNavComponent(
    componentContext: ComponentContext,
    private val userId: UserId,
    private val profileFactory: (ComponentContext, UserId) -> IProfileComponent,
    private val listingDetailsFactory: (ComponentContext, ListingId) -> IListingDetailsComponent,
    private val settingsFactory: (ComponentContext) -> ISettingsComponent,
    private val initialStack: List<ProfileNavConfiguration>
) : IProfileNavComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        userId: UserId,
        profileFactory: (ComponentContext, UserId) -> IProfileComponent,
        listingDetailsFactory: (ComponentContext, ListingId) -> IListingDetailsComponent,
        settingsFactory: (ComponentContext) -> ISettingsComponent
    ) : this(
        componentContext,
        userId,
        profileFactory,
        listingDetailsFactory,
        settingsFactory,
        listOf(ProfileNavConfiguration.Profile(userId))
    )

    private val navigation = StackNavigation<ProfileNavConfiguration>()

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    override val childStack: Value<ChildStack<ProfileNavConfiguration, ProfileNavChildComponent>> = childStack(
        source = navigation,
        serializer = ProfileNavConfiguration.serializer(),
        initialStack = { initialStack },
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val canNavigateBack: Value<Boolean> = childStack.map { it.backStack.isNotEmpty() }

    @OptIn(ExperimentalDecomposeApi::class, ExperimentalUuidApi::class)
    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = navigation,
        stack = childStack,
        serializer = ProfileNavConfiguration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
        childSelector = { null }
    )

    init {
        childStack.subscribe { stack ->
            when (val activeComponent = stack.active.instance) {
                is ProfileNavChildComponent.Profile -> {
                    activeComponent.component.actions.subscribe { _actions.value = it }
                }

                is ProfileNavChildComponent.ListingDetails -> {
                    activeComponent.component.actions.subscribe { _actions.value = it }
                }

                is ProfileNavChildComponent.Settings -> {
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

    override fun navigateToSettings() {
        navigation.bringToFront(ProfileNavConfiguration.Settings)
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(
        configuration: ProfileNavConfiguration,
        componentContext: ComponentContext
    ): ProfileNavChildComponent {
        return when (configuration) {
            is ProfileNavConfiguration.Profile -> ProfileNavChildComponent.Profile(
                profileFactory(componentContext, configuration.userId)
            )

            is ProfileNavConfiguration.ListingDetails -> ProfileNavChildComponent.ListingDetails(
                listingDetailsFactory(componentContext, configuration.listingId)
            )

            is ProfileNavConfiguration.Settings -> ProfileNavChildComponent.Settings(
                settingsFactory(componentContext)
            )
        }
    }
}