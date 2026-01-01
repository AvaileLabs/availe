package com.availelabs.availe.features.chat.list

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.repositories.IMessagingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IChatListComponent {
    val state: StateFlow<ChatListStore.State>
    fun onConversationSelected(jid: Jid)
}

class ChatListComponent(
    componentContext: ComponentContext,
    messagingRepository: IMessagingRepository,
    private val onConversationSelectedCallback: (Jid) -> Unit
) : IChatListComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ChatListStoreFactory(
            storeFactory = DefaultStoreFactory(),
            messagingRepository = messagingRepository
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ChatListStore.State> = store.stateFlow

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ChatListStore.Label.ConversationSelected -> onConversationSelectedCallback(label.jid)
                }
            }
        }
    }

    override fun onConversationSelected(jid: Jid) {
        store.accept(ChatListStore.Intent.SelectConversation(jid))
    }
}