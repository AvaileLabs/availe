package com.availelabs.availe.chat.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.availelabs.availe.chat.details.ChatComponent
import com.availelabs.availe.chat.details.IChatComponent
import com.availelabs.availe.chat.list.ChatListComponent
import com.availelabs.availe.chat.list.IChatListComponent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalDecomposeApi::class)
interface IChatRootComponent : WebNavigationOwner {
    @OptIn(ExperimentalDecomposeApi::class)
    val panels: Value<ChildPanels<ChatRootConfiguration, IChatListComponent, ChatRootConfiguration, IChatComponent, Nothing, Nothing>>
    val canNavigateBack: Value<Boolean>

    @OptIn(ExperimentalDecomposeApi::class)
    fun setMode(mode: ChildPanelsMode)
    fun selectConversation(jid: Jid)
    fun onBackClicked()
    fun reset()
}

@OptIn(ExperimentalDecomposeApi::class)
class ChatRootComponent(
    componentContext: ComponentContext,
    private val messagingRepository: IMessagingRepository,
    private val sessionManager: ISessionRepository,
    private val initialJid: Jid? = null,
    private val navigator: IChatNavigator
) : IChatRootComponent, ComponentContext by componentContext {

    private val navigation = PanelsNavigation<ChatRootConfiguration.List, ChatRootConfiguration.Details, Nothing>()

    // We keep a private typed reference to allow childPanelsWebNavigation to match types correctly with the navigator.
    // The public property upcasts to the sealed interface.
    @OptIn(ExperimentalSerializationApi::class)
    private val _panels = childPanels(
        source = navigation,
        serializers = ChatRootConfiguration.List.serializer() to ChatRootConfiguration.Details.serializer(),
        initialPanels = {
            if (initialJid != null) {
                Panels(
                    main = ChatRootConfiguration.List,
                    details = ChatRootConfiguration.Details(initialJid),
                    mode = ChildPanelsMode.DUAL
                )
            } else {
                Panels(
                    main = ChatRootConfiguration.List,
                    mode = ChildPanelsMode.SINGLE
                )
            }
        },
        mainFactory = ::createList,
        detailsFactory = ::createDetails
    )

    @OptIn(ExperimentalSerializationApi::class)
    override val panels: Value<ChildPanels<ChatRootConfiguration, IChatListComponent, ChatRootConfiguration, IChatComponent, Nothing, Nothing>> =
        _panels as Value<ChildPanels<ChatRootConfiguration, IChatListComponent, ChatRootConfiguration, IChatComponent, Nothing, Nothing>>

    override val canNavigateBack: Value<Boolean> = panels.map { state ->
        state.mode == ChildPanelsMode.SINGLE && state.details != null
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val webNavigation: WebNavigation<*> = childPanelsWebNavigation(
        navigator = navigation,
        panels = _panels,
        serializers = ChatRootConfiguration.List.serializer() to ChatRootConfiguration.Details.serializer(),
        pathMapper = { state -> state.details?.configuration?.jid?.value },
        childSelector = { null }
    )

    override fun setMode(mode: ChildPanelsMode) {
        navigation.setMode(mode)
    }

    override fun selectConversation(jid: Jid) {
        navigation.activateDetails(ChatRootConfiguration.Details(jid))
    }

    override fun onBackClicked() {
        navigation.dismissDetails()
    }

    override fun reset() {
        navigation.dismissDetails()
    }

    private fun createList(
        configuration: ChatRootConfiguration.List,
        componentContext: ComponentContext
    ): IChatListComponent {
        return ChatListComponent(
            componentContext = componentContext,
            messagingRepository = messagingRepository,
            sessionManager = sessionManager,
            onConversationSelectedCallback = { jid ->
                navigation.activateDetails(ChatRootConfiguration.Details(jid))
            }
        )
    }

    private fun createDetails(
        configuration: ChatRootConfiguration.Details,
        componentContext: ComponentContext
    ): IChatComponent {
        return ChatComponent(
            componentContext = componentContext,
            recipientJid = configuration.jid,
            messagingRepository = messagingRepository,
            sessionManager = sessionManager,
            onNavigateBack = {
                navigation.dismissDetails()
            }
        )
    }
}