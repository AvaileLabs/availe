package com.availelabs.availe.features.chat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.data.sources.remote.IMessagingRemoteDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IChatComponent {
    val state: StateFlow<ChatStore.State>
    fun onIntent(intent: ChatStore.Intent)
    fun onBackClicked()
}

class ChatComponent(
    componentContext: ComponentContext,
    private val recipientId: String,
    private val messagingRemoteDataSource: IMessagingRemoteDataSource,
    private val sessionManager: ISessionManager,
    private val onNavigateBack: () -> Unit
) : IChatComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ChatStoreFactory(
            storeFactory = DefaultStoreFactory(),
            messagingRemoteDataSource = messagingRemoteDataSource,
            sessionManager = sessionManager,
            recipientId = recipientId
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ChatStore.State> = store.stateFlow

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