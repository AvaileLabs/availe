package com.availelabs.availe.features.root

import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listingdetails.IListingDetailsComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsComponent
import com.availelabs.availe.features.main.MainStore
import com.availelabs.availe.features.main.MainStoreFactory
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalDecomposeApi::class)
interface IRootComponent : WebNavigationOwner, BackHandlerOwner {
    val childStack: Value<ChildStack<Configuration, ChildComponent>>
    val authenticationState: StateFlow<MainStore.State>
    val backDispatcher: BackDispatcher?
    fun onAuthenticationIntent(intent: MainStore.Intent)
    fun onBackClicked()
}

@OptIn(ExperimentalDecomposeApi::class, ExperimentalUuidApi::class)
class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: IApplicationContainer,
    private val deepLinkUrl: String? = null
) : IRootComponent, ComponentContext by componentContext {

    constructor(
        componentContext: ComponentContext,
        appContainer: IApplicationContainer
    ) : this(componentContext, appContainer, null)

    override val backDispatcher: BackDispatcher? = backHandler as? BackDispatcher

    private val mainStore = instanceKeeper.getStore {
        MainStoreFactory(
            storeFactory = DefaultStoreFactory(),
            authenticationRepository = appContainer.authenticationRepository,
            sessionManager = appContainer.sessionManager
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val authenticationState: StateFlow<MainStore.State> = mainStore.stateFlow

    private val navigation = StackNavigation<Configuration>()

    override val childStack: Value<ChildStack<Configuration, ChildComponent>> = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialStack = {
            var initialStack: List<Configuration> = listOf(Configuration.Home)
            if (deepLinkUrl != null) {
                try {
                    val uniformResourceLocator = Url(deepLinkUrl)
                    val deepLink = parseDeepLink(uniformResourceLocator)
                    val configuration = deepLink.toConfiguration()

                    if (configuration is Configuration.ListingDetails) {
                        initialStack = listOf(Configuration.Home, configuration)
                    }
                } catch (_: Exception) {
                    // Ignore invalid URLs and fallback to default stack
                }
            }
            initialStack
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

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onAuthenticationIntent(intent: MainStore.Intent) {
        mainStore.accept(intent)
    }

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChildComponent(configuration: Configuration, componentContext: ComponentContext): ChildComponent {
        return when (configuration) {
            Configuration.Home -> ChildComponent.Home(
                HomeComponent(
                    componentContext = componentContext,
                    listingRepository = appContainer.listingRepository,
                    sessionManager = appContainer.sessionManager,
                    onNavigateToListingDetails = { listingId ->
                        navigation.push(Configuration.ListingDetails(listingId))
                    }
                )
            )

            is Configuration.ListingDetails -> ChildComponent.ListingDetails(
                ListingDetailsComponent(
                    componentContext = componentContext,
                    listingId = configuration.listingId,
                    listingRepository = appContainer.listingRepository,
                    onNavigateBack = { navigation.pop() }
                )
            )
        }
    }

    private sealed interface DeepLink {
        data object Home : DeepLink
        data class Listing(val id: ListingId) : DeepLink
    }

    private fun parseDeepLink(uniformResourceLocator: Url): DeepLink {
        val pathSegments = uniformResourceLocator.segments

        return if (pathSegments.isEmpty()) {
            DeepLink.Home
        } else if (pathSegments.size == 2 && pathSegments[0] == Configuration.ListingDetails.ROUTE_SEGMENT_NAME) {
            try {
                val uuid = Uuid.parse(pathSegments[1])
                DeepLink.Listing(ListingId(uuid))
            } catch (_: IllegalArgumentException) {
                DeepLink.Home
            }
        } else {
            DeepLink.Home
        }
    }

    private fun DeepLink.toConfiguration(): Configuration {
        return when (this) {
            DeepLink.Home -> Configuration.Home
            is DeepLink.Listing -> Configuration.ListingDetails(this.id)
        }
    }

    private fun getPathForConfiguration(configuration: Configuration): String {
        return when (configuration) {
            Configuration.Home -> "/"
            is Configuration.ListingDetails -> "/${Configuration.ListingDetails.ROUTE_SEGMENT_NAME}/${configuration.listingId.value}"
        }
    }
}

sealed interface ChildComponent {
    data class Home(val component: IHomeComponent) : ChildComponent
    data class ListingDetails(val component: IListingDetailsComponent) : ChildComponent
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

    @Serializable
    data class ListingDetails(val listingId: ListingId) : Configuration {
        @Transient
        override val displayName: String = "Details"

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, "Details Screen")

        companion object {
            const val ROUTE_SEGMENT_NAME = "listing"
        }
    }
}