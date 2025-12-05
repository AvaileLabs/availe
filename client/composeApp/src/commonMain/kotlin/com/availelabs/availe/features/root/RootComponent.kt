@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.root

import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
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
import com.availelabs.availe.features.chat.ChatComponent
import com.availelabs.availe.features.chat.IChatComponent
import com.availelabs.availe.features.home.HomeComponent
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listingdetails.IListingDetailsComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsComponent
import com.availelabs.availe.features.main.MainStore
import com.availelabs.availe.features.main.MainStoreFactory
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
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
            RootNavigationMapper.getStackForDeepLink(deepLinkUrl)
        },
        childFactory = ::createChildComponent
    )

    override val webNavigation: WebNavigation<*> = childStackWebNavigation(
        navigator = navigation,
        stack = childStack,
        serializer = Configuration.serializer(),
        pathMapper = { child -> child.configuration.toUrlPath() },
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

    @OptIn(DelicateDecomposeApi::class)
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
                    onNavigateBack = { navigation.pop() },
                    onNavigateToChat = { recipientId ->
                        navigation.push(Configuration.Chat(recipientId))
                    }
                )
            )

            is Configuration.Chat -> ChildComponent.Chat(
                ChatComponent(
                    componentContext = componentContext,
                    recipientId = configuration.recipientId,
                    sessionManager = appContainer.sessionManager,
                    messagingRepository = appContainer.messagingRepository,
                    onNavigateBack = { navigation.pop() }
                )
            )
        }
    }
}

sealed interface ChildComponent {
    data class Home(val component: IHomeComponent) : ChildComponent
    data class ListingDetails(val component: IListingDetailsComponent) : ChildComponent
    data class Chat(val component: IChatComponent) : ChildComponent
}

data class NavIcon(
    val icon: ImageVector,
    val contentDescription: String
)

@Serializable
sealed interface Configuration {
    val displayName: String
    val navigationIcon: NavIcon
    val canonicalParent: Configuration? get() = null

    fun toUrlPath(): String

    interface RouteParser<out C : Configuration> {
        fun parse(segments: List<String>): C?
    }

    @Serializable
    data object Home : Configuration {
        @Transient
        override val displayName: String = "Availe"

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, "Home Screen")

        override fun toUrlPath(): String = "/"
    }

    @Serializable
    data class ListingDetails(val listingId: ListingId) : Configuration {
        @Transient
        override val displayName: String = "Details"

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.House, "Details Screen")

        override val canonicalParent: Configuration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${listingId.value}"

        @OptIn(ExperimentalUuidApi::class)
        companion object : RouteParser<ListingDetails> {
            const val ROUTE_SEGMENT_NAME = "listing"

            override fun parse(segments: List<String>): ListingDetails? {
                if (segments.isEmpty()) return null
                return try {
                    val uuidString = segments.first()
                    val uuid = Uuid.parse(uuidString)
                    ListingDetails(ListingId(uuid))
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    @Serializable
    data class Chat(val recipientId: String) : Configuration {
        @Transient
        override val displayName: String = "Chat"

        @Transient
        override val navigationIcon: NavIcon = NavIcon(Lucide.MessageCircle, "Chat Screen")

        override val canonicalParent: Configuration = Home

        override fun toUrlPath(): String = "/chat/$recipientId"
    }
}