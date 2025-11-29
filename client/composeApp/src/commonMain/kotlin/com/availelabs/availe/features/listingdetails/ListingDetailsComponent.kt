package com.availelabs.availe.features.listingdetails

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.repository.IListingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IListingDetailsComponent {
    val state: StateFlow<ListingDetailsStore.State>
    fun onIntent(intent: ListingDetailsStore.Intent)
    fun onBackClicked()
}

class ListingDetailsComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId,
    private val listingRepository: IListingRepository,
    private val onNavigateBack: () -> Unit
) : IListingDetailsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ListingDetailsStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            listingId = listingId
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ListingDetailsStore.State> = store.stateFlow

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onIntent(intent: ListingDetailsStore.Intent) {
        store.accept(intent)
    }

    override fun onBackClicked() {
        onNavigateBack()
    }
}