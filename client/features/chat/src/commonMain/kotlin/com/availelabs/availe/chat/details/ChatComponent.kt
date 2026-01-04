package com.availelabs.availe.chat.details

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IChatComponent {
    val state: StateFlow<ChatStore.State>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ChatStore.Intent)
    fun onBackClicked()
}

class ChatComponent(
    componentContext: ComponentContext,
    private val recipientJid: Jid,
    private val messagingRepository: IMessagingRepository,
    private val sessionManager: ISessionRepository,
    private val onNavigateBack: () -> Unit // Kept as lambda for internal panel navigation
) : IChatComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ChatStoreFactory(
            storeFactory = DefaultStoreFactory(),
            messagingRepository = messagingRepository,
            sessionManager = sessionManager,
            recipientJid = recipientJid
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ChatStore.State> = store.stateFlow

    override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }
        store.accept(ChatStore.Intent.Connect)
    }

    override fun onIntent(intent: ChatStore.Intent) {
        store.accept(intent)
    }

    override fun onBackClicked() {
        store.accept(ChatStore.Intent.Disconnect)
        onNavigateBack()
    }
}