@file:OptIn(ExperimentalUuidApi::class, ExperimentalDecomposeApi::class)

package com.availelabs.availe.features.appshell

import androidx.compose.ui.graphics.vector.ImageVector
import availe.client.composeapp.generated.resources.*
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.features.chat.ChatComponent
import com.availelabs.availe.features.chat.IChatComponent
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listingdetails.IListingDetailsComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsComponent
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.LayoutMode
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import kotlin.uuid.ExperimentalUuidApi

interface IAppShellComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>>
    val chatOverlay: Value<ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>>
    val authenticationState: Value<AuthenticationState>
    val isAuthenticationLoading: Value<Boolean>
    val backDispatcher: BackDispatcher?

    fun onLayoutModeChanged(mode: LayoutMode)
    fun expandChatOverlay()
    fun dismissChatOverlay()
    fun onBackClicked()
    fun onLoginClicked()
    fun onCancelLoginClicked()
    fun onLogoutClicked()
    fun onHomeTabClicked()
}

class AppShellComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val initialIntent: AppShellIntent,
    override val backDispatcher: BackDispatcher?,
    override val isAuthenticationLoading: Value<Boolean>,
    private val onExplicitLogin: () -> Unit,
    private val onCancelLogin: () -> Unit,
    private val onShowLoginGuard: () -> Unit
) : IAppShellComponent, ComponentContext by componentContext {

    private val childStackNavigation = StackNavigation<AppShellConfiguration>()
    private val chatOverlayNavigation = SlotNavigation<AppShellConfiguration.Chat>()
    private var layoutMode = LayoutMode.SinglePane

    override val authenticationState: Value<AuthenticationState> = appContainer.sessionManager.authenticationState

    override val childStack: Value<ChildStack<AppShellConfiguration, AppShellChildComponent>> = childStack(
        source = childStackNavigation,
        serializer = AppShellConfiguration.serializer(),
        initialStack = { calculateInitialStack(initialIntent) },
        childFactory = ::createChildComponent
    )

    override val chatOverlay: Value<ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>> = childSlot(
        source = chatOverlayNavigation,
        serializer = AppShellConfiguration.Chat.serializer(),
        initialConfiguration = { calculateInitialOverlay(initialIntent) },
        handleBackButton = false,
        childFactory = ::createChatOverlayComponent
    )

    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = childStackNavigation,
        stack = childStack,
        serializer = AppShellConfiguration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
        childSelector = {
            null
        }
    )

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        val dismissOverlayOnLogoutObserver: (AuthenticationState) -> Unit = { state ->
            if (state !is AuthenticationState.LoggedIn) {
                dismissChatOverlay()
            }
        }
        val authenticationSubscription = authenticationState.subscribe(dismissOverlayOnLogoutObserver)
        lifecycle.doOnDestroy { authenticationSubscription.cancel() }
    }

    override fun onLayoutModeChanged(mode: LayoutMode) {
        if (layoutMode == mode) return
        layoutMode = mode

        if (mode == LayoutMode.SinglePane) {
            val overlayChild = chatOverlay.value.child
            if (overlayChild != null) {
                val configuration = overlayChild.configuration
                chatOverlayNavigation.dismiss()
                childStackNavigation.bringToFront(configuration)
            }
        } else {
            val activeConfiguration = childStack.value.active.configuration
            if (activeConfiguration is AppShellConfiguration.Chat) {
                childStackNavigation.pop()
                chatOverlayNavigation.activate(activeConfiguration)
            }
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun expandChatOverlay() {
        val currentSlot = chatOverlay.value.child
        if (currentSlot != null) {
            val configuration = currentSlot.configuration
            chatOverlayNavigation.dismiss()
            childStackNavigation.bringToFront(configuration)
        }
    }

    override fun dismissChatOverlay() {
        chatOverlayNavigation.dismiss()
    }

    override fun onBackClicked() {
        childStackNavigation.pop()
    }

    override fun onLoginClicked() {
        onExplicitLogin()
    }

    override fun onCancelLoginClicked() {
        onCancelLogin()
    }

    override fun onLogoutClicked() {
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        scope.launch {
            appContainer.authenticationRepository.logout()
        }
    }

    override fun onHomeTabClicked() {
        dismissChatOverlay()
        childStackNavigation.bringToFront(AppShellConfiguration.Home)
    }

    private fun calculateInitialStack(intent: AppShellIntent): List<AppShellConfiguration> {
        return when (intent) {
            is AppShellIntent.Home -> listOf(AppShellConfiguration.Home)
            is AppShellIntent.ViewingListing -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.ListingDetails(intent.listingId)
            )

            is AppShellIntent.ViewingListingWithChat -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.ListingDetails(intent.listingId)
            )

            is AppShellIntent.Chatting -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.Chat(intent.recipientJid)
            )
        }
    }

    private fun calculateInitialOverlay(intent: AppShellIntent): AppShellConfiguration.Chat? {
        return when (intent) {
            is AppShellIntent.ViewingListingWithChat -> AppShellConfiguration.Chat(intent.recipientJid)
            else -> null
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun createChildComponent(
        configuration: AppShellConfiguration,
        componentContext: ComponentContext
    ): AppShellChildComponent {
        return when (configuration) {
            AppShellConfiguration.Home -> AppShellChildComponent.Home(
                HomeComponent(
                    componentContext = componentContext,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    onNavigateToListingDetails = { listingId ->
                        childStackNavigation.pushNew(AppShellConfiguration.ListingDetails(listingId))
                    },
                    messageService = appContainer.messageService
                )
            )

            is AppShellConfiguration.ListingDetails -> AppShellChildComponent.ListingDetails(
                ListingDetailsComponent(
                    componentContext = componentContext,
                    listingId = configuration.listingId,
                    listingRepository = appContainer.listingRepository,
                    onNavigateBack = { childStackNavigation.pop() },
                    onNavigateToChat = { recipientJid ->
                        val isAuthenticated =
                            appContainer.sessionManager.authenticationState.value is AuthenticationState.LoggedIn
                        if (isAuthenticated) {
                            if (layoutMode == LayoutMode.MultiPane) {
                                chatOverlayNavigation.activate(AppShellConfiguration.Chat(recipientJid))
                            } else {
                                childStackNavigation.bringToFront(AppShellConfiguration.Chat(recipientJid))
                            }
                        } else {
                            onShowLoginGuard()
                        }
                    }
                )
            )

            is AppShellConfiguration.Chat -> AppShellChildComponent.Chat(
                ChatComponent(
                    componentContext = componentContext,
                    recipientJid = configuration.recipientJid,
                    sessionManager = appContainer.sessionManager,
                    messagingRepository = appContainer.messagingRepository,
                    onNavigateBack = { childStackNavigation.pop() }
                )
            )
        }
    }

    private fun createChatOverlayComponent(
        configuration: AppShellConfiguration.Chat,
        componentContext: ComponentContext
    ): AppShellChildComponent.Chat {
        return AppShellChildComponent.Chat(
            ChatComponent(
                componentContext = componentContext,
                recipientJid = configuration.recipientJid,
                sessionManager = appContainer.sessionManager,
                messagingRepository = appContainer.messagingRepository,
                onNavigateBack = { chatOverlayNavigation.dismiss() }
            )
        )
    }
}

sealed interface AppShellChildComponent {
    val actions: Value<List<AppBarAction>>

    data class Home(val component: IHomeComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class ListingDetails(val component: IListingDetailsComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Chat(val component: IChatComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }
}

data class NavIcon(
    val icon: ImageVector,
    val contentDescription: StringResource
)

@Serializable
sealed interface AppShellConfiguration {
    val titleResource: StringResource
    val navigationIcon: NavIcon
    val canonicalParent: AppShellConfiguration? get() = null

    fun toUrlPath(): String

    @Serializable
    data object Home : AppShellConfiguration {
        @Transient
        override val titleResource: StringResource = Res.string.navigation_home

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, Res.string.accessibility_navigation_home)

        override fun toUrlPath(): String = "/"
    }

    @Serializable
    data class ListingDetails(val listingId: ListingId) : AppShellConfiguration {
        @Transient
        override val titleResource: StringResource = Res.string.navigation_details

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, Res.string.accessibility_navigation_details)

        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${listingId.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "listing"
        }
    }

    @Serializable
    data class Chat(val recipientJid: Jid) : AppShellConfiguration {
        @Transient
        override val titleResource: StringResource = Res.string.navigation_chat

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.MessageCircle, Res.string.accessibility_navigation_chat)

        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${recipientJid.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "chat"
        }
    }
}