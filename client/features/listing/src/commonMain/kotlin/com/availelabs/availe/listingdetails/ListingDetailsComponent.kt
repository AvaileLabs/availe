package com.availelabs.availe.listingdetails

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IListingDetailsComponent {
    val state: StateFlow<ListingDetailsStore.State>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ListingDetailsStore.Intent)
    fun onBackClicked()
    fun onContactSellerClicked(sellerEmail: Email)
    fun onProfileClicked(userId: UserId)
    fun onEditListingClicked()
}

class ListingDetailsComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val navigator: IListingDetailsNavigator
) : IListingDetailsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ListingDetailsStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            sessionManager = sessionManager,
            listingId = listingId
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ListingDetailsStore.State> = store.stateFlow

    override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onIntent(intent: ListingDetailsStore.Intent) {
        store.accept(intent)
    }

    override fun onBackClicked() {
        navigator.navigateBack()
    }

    override fun onContactSellerClicked(sellerEmail: Email) {
        val jid = Jid(sellerEmail.value)
        navigator.navigateToChat(jid)
    }

    override fun onProfileClicked(userId: UserId) {
        navigator.navigateToProfile(userId)
    }

    override fun onEditListingClicked() {
        navigator.navigateToEditListing(listingId)
    }
}