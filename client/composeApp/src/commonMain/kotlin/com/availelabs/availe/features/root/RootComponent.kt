package com.availelabs.availe.features.root

import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.di.ApplicationContainer
import com.availelabs.availe.features.favorites.FavoritesComponent
import com.availelabs.availe.features.favorites.IFavoritesComponent
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listings.IMyListingsComponent
import com.availelabs.availe.features.listings.MyListingsComponent
import com.availelabs.availe.features.messages.IMessagesComponent
import com.availelabs.availe.features.messages.MessagesComponent
import com.availelabs.availe.features.profile.IProfileComponent
import com.availelabs.availe.features.profile.ProfileComponent
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class TopLevelDestination(
    val configuration: Configuration,
    val name: String,
    val icon: ImageVector
)

sealed interface OverlayState {
    data object None : OverlayState
    data object Search : OverlayState
    data object Notifications : OverlayState
}

interface IRootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val topLevelDestinations: List<TopLevelDestination>
    val overlayState: Value<OverlayState>

    fun onNavigate(configuration: Configuration)
    fun onSearchClicked()
    fun onNotificationClicked()
    fun onDismissOverlay()
    fun openUrl(url: String)
    fun onPostClicked()
}

class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: ApplicationContainer
) : IRootComponent, ComponentContext by componentContext {

    override val topLevelDestinations = listOf(
        TopLevelDestination(Configuration.Home, "Home", Lucide.House),
        TopLevelDestination(Configuration.Favorites, "Favorites", Lucide.Heart),
        TopLevelDestination(Configuration.MyListings, "My Listings", Lucide.Package),
        TopLevelDestination(Configuration.Messages, "Messages", Lucide.MessageSquare),
        TopLevelDestination(Configuration.Profile, "Profile", Lucide.User)
    )

    private val navigation = StackNavigation<Configuration>()
    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    override val childStack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Home,
        childFactory = ::createChild,
    )

    private val _overlayState = MutableValue<OverlayState>(OverlayState.None)
    override val overlayState: Value<OverlayState> = _overlayState

    var urlOpener: ((String) -> Unit)? = null

    override fun onNavigate(configuration: Configuration) {
        navigation.bringToFront(configuration)
        _overlayState.update { OverlayState.None }
    }

    override fun onSearchClicked() {
        _overlayState.update {
            if (it is OverlayState.Search) OverlayState.None else OverlayState.Search
        }
    }

    override fun onNotificationClicked() {
        _overlayState.update {
            if (it is OverlayState.Notifications) OverlayState.None else OverlayState.Notifications
        }
    }

    override fun onDismissOverlay() {
        _overlayState.update { OverlayState.None }
    }

    override fun openUrl(url: String) {
        urlOpener?.invoke(url)
    }

    override fun onPostClicked() {
        println("Post clicked")
    }

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child {
        return when (configuration) {
            is Configuration.Home -> Child.Home(createHomeScreenComponent(componentContext))
            is Configuration.Favorites -> Child.Favorites(FavoritesComponent(componentContext))
            is Configuration.MyListings -> Child.MyListings(MyListingsComponent(componentContext))
            is Configuration.Messages -> Child.Messages(MessagesComponent(componentContext))
            is Configuration.Profile -> Child.Profile(createProfileComponent(componentContext))
            is Configuration.Settings -> Child.Settings(SettingsComponent(componentContext))
        }
    }

    private fun createHomeScreenComponent(componentContext: ComponentContext): IHomeComponent {
        return HomeComponent(
            componentContext = componentContext,
            apiClient = appContainer.apiClient,
            onLogout = {
                scope.launch {
                    appContainer.authenticationService.logout()
                }
            }
        )
    }

    private fun createProfileComponent(componentContext: ComponentContext): IProfileComponent {
        return ProfileComponent(
            componentContext = componentContext,
            authService = appContainer.authenticationService,
            onOpenUrl = { url -> openUrl(url) }
        )
    }
}

sealed interface Child {
    data class Home(val component: IHomeComponent) : Child
    data class Favorites(val component: IFavoritesComponent) : Child
    data class MyListings(val component: IMyListingsComponent) : Child
    data class Messages(val component: IMessagesComponent) : Child
    data class Profile(val component: IProfileComponent) : Child
    data class Settings(val component: ISettingsComponent) : Child
}

@Serializable
sealed interface Configuration {
    @Serializable
    data object Home : Configuration

    @Serializable
    data object Favorites : Configuration

    @Serializable
    data object MyListings : Configuration

    @Serializable
    data object Messages : Configuration

    @Serializable
    data object Profile : Configuration

    @Serializable
    data object Settings : Configuration
}

interface ISettingsComponent
class SettingsComponent(componentContext: ComponentContext) : ISettingsComponent,
    ComponentContext by componentContext