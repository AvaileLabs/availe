package com.availelabs.availe.listingfeed

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.decompose.asValue
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.repositories.IEventRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import kotlinx.coroutines.*

interface IListingFeedComponent {
    val state: Value<ListingFeedStore.State>
    fun onIntent(intent: ListingFeedStore.Intent)
}

class ListingFeedComponent(
    componentContext: ComponentContext,
    private val feedType: ListingFeedType,
    private val listingRepository: IListingRepository,
    private val eventRepository: IEventRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val onNavigateToListingDetails: (ListingId) -> Unit
) : IListingFeedComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ListingFeedStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            eventRepository = eventRepository,
            sessionManager = sessionManager,
            feedType = feedType
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: Value<ListingFeedStore.State> = store.stateFlow.asValue(lifecycle)

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ListingFeedStore.Label.NavigateToListingDetails -> onNavigateToListingDetails(label.listingId)
                    is ListingFeedStore.Label.DisplayError -> {
                        messageService.show(ToastMessage.Error(label.error))
                    }
                }
            }
        }
    }

    override fun onIntent(intent: ListingFeedStore.Intent) {
        store.accept(intent)
    }
}