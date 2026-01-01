package com.availelabs.availe.features.profilenav

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
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.features.listingdetails.IListingDetailsComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsComponent
import com.availelabs.availe.features.profile.IProfileComponent
import com.availelabs.availe.features.profile.ProfileComponent
import com.availelabs.availe.features.settings.ISettingsComponent
import com.availelabs.availe.features.settings.SettingsComponent
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
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
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val authenticationRepository: IAuthenticationRepository,
    private val messageService: IMessageService,
    private val onNavigateToChat: (UserId) -> Unit,
    private val onNavigateToChatByJid: (Jid) -> Unit,
    private val onOpenUrl: (String) -> Unit,
    private val initialStack: List<ProfileNavConfiguration>
) : IProfileNavComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        userId: UserId,
        listingRepository: IListingRepository,
        sessionManager: ISessionRepository,
        authenticationRepository: IAuthenticationRepository,
        messageService: IMessageService,
        onNavigateToChat: (UserId) -> Unit,
        onNavigateToChatByJid: (Jid) -> Unit,
        onOpenUrl: (String) -> Unit
    ) : this(
        componentContext,
        userId,
        listingRepository,
        sessionManager,
        authenticationRepository,
        messageService,
        onNavigateToChat,
        onNavigateToChatByJid,
        onOpenUrl,
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
                ProfileComponent(
                    componentContext = componentContext,
                    targetUserId = configuration.userId,
                    listingRepository = listingRepository,
                    sessionManager = sessionManager,
                    messageService = messageService,
                    onNavigateBack = {
                    },
                    onNavigateToListing = { listingId ->
                        navigation.push(ProfileNavConfiguration.ListingDetails(listingId))
                    },
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToSettings = {
                        navigation.push(ProfileNavConfiguration.Settings)
                    }
                )
            )

            is ProfileNavConfiguration.ListingDetails -> ProfileNavChildComponent.ListingDetails(
                ListingDetailsComponent(
                    componentContext = componentContext,
                    listingId = configuration.listingId,
                    listingRepository = listingRepository,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToChat = onNavigateToChatByJid,
                    onNavigateToProfile = { userId ->
                        navigation.bringToFront(ProfileNavConfiguration.Profile(userId))
                    }
                )
            )

            is ProfileNavConfiguration.Settings -> ProfileNavChildComponent.Settings(
                SettingsComponent(
                    componentContext = componentContext,
                    storeFactory = DefaultStoreFactory(),
                    sessionManager = sessionManager,
                    authenticationRepository = authenticationRepository,
                    onOpenUrl = onOpenUrl
                )
            )
        }
    }
}