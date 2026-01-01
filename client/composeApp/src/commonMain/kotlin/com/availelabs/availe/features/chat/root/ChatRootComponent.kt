package com.availelabs.availe.features.chat.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.features.chat.details.ChatComponent
import com.availelabs.availe.features.chat.details.IChatComponent
import com.availelabs.availe.features.chat.list.ChatListComponent
import com.availelabs.availe.features.chat.list.IChatListComponent
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalDecomposeApi::class)
interface IChatRootComponent {
    val panels: Value<ChildPanels<ChatRootConfiguration, IChatListComponent, ChatRootConfiguration, IChatComponent, Nothing, Nothing>>
    fun setMode(mode: ChildPanelsMode)
    fun selectConversation(jid: Jid)
}

@OptIn(ExperimentalDecomposeApi::class)
class ChatRootComponent(
    componentContext: ComponentContext,
    private val messagingRepository: IMessagingRepository,
    private val sessionManager: ISessionRepository,
    private val initialJid: Jid? = null,
    private val onNavigateBack: () -> Unit
) : IChatRootComponent, ComponentContext by componentContext {

    private val navigation = PanelsNavigation<ChatRootConfiguration.List, ChatRootConfiguration.Details, Nothing>()

    @OptIn(ExperimentalSerializationApi::class)
    override val panels: Value<ChildPanels<ChatRootConfiguration, IChatListComponent, ChatRootConfiguration, IChatComponent, Nothing, Nothing>> =
        childPanels(
            source = navigation,
            serializers = ChatRootConfiguration.List.serializer() to ChatRootConfiguration.Details.serializer(),
            initialPanels = {
                if (initialJid != null) {
                    com.arkivanov.decompose.router.panels.Panels(
                        main = ChatRootConfiguration.List,
                        details = ChatRootConfiguration.Details(initialJid),
                        mode = ChildPanelsMode.DUAL
                    )
                } else {
                    com.arkivanov.decompose.router.panels.Panels(
                        main = ChatRootConfiguration.List,
                        mode = ChildPanelsMode.SINGLE
                    )
                }
            },
            mainFactory = ::createList,
            detailsFactory = ::createDetails
        )

    override fun setMode(mode: ChildPanelsMode) {
        navigation.setMode(mode)
    }

    override fun selectConversation(jid: Jid) {
        navigation.activateDetails(ChatRootConfiguration.Details(jid))
    }

    private fun createList(
        configuration: ChatRootConfiguration.List,
        componentContext: ComponentContext
    ): IChatListComponent {
        return ChatListComponent(
            componentContext = componentContext,
            messagingRepository = messagingRepository,
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
                // In single pane mode, Decompose handles the back navigation from details to list automatically.
                // We could explicitly call dismissDetails() here if custom logic is needed.
            }
        )
    }
}