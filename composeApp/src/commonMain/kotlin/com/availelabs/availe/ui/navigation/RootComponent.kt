package com.availelabs.availe.ui.navigation

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
import com.availelabs.availe.ui.screens.home.HomeScreenComponent
import com.availelabs.availe.ui.screens.home.IHomeScreenComponent
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
}

class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: ApplicationContainer
) : IRootComponent, ComponentContext by componentContext {

    override val topLevelDestinations = listOf(
        TopLevelDestination(Configuration.Home, "Home", Lucide.House),
        TopLevelDestination(Configuration.Orders, "Orders", Lucide.ChartNoAxesCombined),
        TopLevelDestination(Configuration.Products, "Products", Lucide.Package),
        TopLevelDestination(Configuration.Settings, "Settings", Lucide.Cog)
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

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child {
        return when (configuration) {
            is Configuration.Home -> Child.Home(createHomeScreenComponent(componentContext))
            is Configuration.Orders -> Child.Orders(OrdersComponent(componentContext))
            is Configuration.Products -> Child.Products(ProductsComponent(componentContext))
            is Configuration.Settings -> Child.Settings(SettingsComponent(componentContext))
        }
    }

    private fun createHomeScreenComponent(componentContext: ComponentContext): IHomeScreenComponent {
        return HomeScreenComponent(
            componentContext = componentContext,
            onLogout = {
                scope.launch {
                    appContainer.authenticationService.logout()
                }
            }
        )
    }
}

sealed interface Child {
    data class Home(val component: IHomeScreenComponent) : Child
    data class Orders(val component: IOrdersComponent) : Child
    data class Products(val component: IProductsComponent) : Child
    data class Settings(val component: ISettingsComponent) : Child
}

@Serializable
sealed interface Configuration {
    @Serializable
    data object Home : Configuration

    @Serializable
    data object Orders : Configuration

    @Serializable
    data object Products : Configuration

    @Serializable
    data object Settings : Configuration
}

interface IOrdersComponent
class OrdersComponent(componentContext: ComponentContext) : IOrdersComponent,
    ComponentContext by componentContext

interface IProductsComponent
class ProductsComponent(componentContext: ComponentContext) : IProductsComponent,
    ComponentContext by componentContext

interface ISettingsComponent
class SettingsComponent(componentContext: ComponentContext) : ISettingsComponent,
    ComponentContext by componentContext