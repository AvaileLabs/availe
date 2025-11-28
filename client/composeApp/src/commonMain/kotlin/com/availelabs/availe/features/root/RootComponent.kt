package com.availelabs.availe.features.root

import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(ExperimentalDecomposeApi::class)
interface IRootComponent : WebNavigationOwner {
    val childStack: Value<ChildStack<*, ChildComponent>>
}

@OptIn(ExperimentalDecomposeApi::class)
class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val deepLinkUrl: String? = null
) : IRootComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        appContainer: IApplicationContainer
    ) : this(componentContext, appContainer, null)

    private val navigation = StackNavigation<Configuration>()

    override val childStack: Value<ChildStack<Configuration, ChildComponent>> = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialStack = {
            if (deepLinkUrl != null) {
                val path = try {
                    Url(deepLinkUrl).encodedPath
                } catch (_: Exception) {
                    deepLinkUrl
                }
                val configuration = getConfigurationForPath(path)
                listOf(configuration)
            } else {
                listOf(Configuration.Home)
            }
        },
        childFactory = ::createChildComponent
    )

    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = navigation,
        stack = childStack,
        serializer = Configuration.serializer(),
        pathMapper = { child -> getPathForConfiguration(child.configuration) },
        childSelector = {
            null
        }
    )

    private fun createChildComponent(config: Configuration, context: ComponentContext): ChildComponent {
        return when (config) {
            Configuration.Home -> ChildComponent.Home(
                HomeComponent(
                    componentContext = context,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    authRepository = appContainer.authRepository,
                    onNavigateToListingDetails = { /* TODO */ }
                )
            )
        }
    }

    private fun getPathForConfiguration(configuration: Configuration): String {
        return when (configuration) {
            Configuration.Home -> "/"
        }
    }

    private fun getConfigurationForPath(path: String): Configuration {
        return when (path.removeSuffix("/")) {
            else -> Configuration.Home
        }
    }
}

sealed interface ChildComponent {
    data class Home(val component: IHomeComponent) : ChildComponent
}

data class NavIcon(
    val icon: ImageVector,
    val contentDescription: String
)

@Serializable
sealed interface Configuration {
    val displayName: String
    val navigationIcon: NavIcon

    @Serializable
    data object Home : Configuration {
        @Transient
        override val displayName: String = "Availe"

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, "Home Screen")
    }
}