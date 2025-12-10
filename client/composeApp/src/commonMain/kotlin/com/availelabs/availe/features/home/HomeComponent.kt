package com.availelabs.availe.features.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.data.repositories.IListingRepository
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IHomeComponent {
    val state: StateFlow<HomeStore.State>
    fun onIntent(intent: HomeStore.Intent)
}

class HomeComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionManager,
    private val onNavigateToListingDetails: (ListingId) -> Unit,
    private val onShowError: (UiText) -> Unit
) : IHomeComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        HomeStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            sessionManager = sessionManager
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<HomeStore.State> = store.stateFlow

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is HomeStore.Label.NavigateToListingDetails -> onNavigateToListingDetails(label.listingId)
                    is HomeStore.Label.DisplayError -> onShowError(label.message)
                }
            }
        }
    }

    override fun onIntent(intent: HomeStore.Intent) {
        store.accept(intent)
    }
}