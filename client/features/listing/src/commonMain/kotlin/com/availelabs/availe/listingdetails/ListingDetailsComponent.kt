package com.availelabs.availe.listingdetails

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.repositories.IListingRepository
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
}

class ListingDetailsComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId,
    private val listingRepository: IListingRepository,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToChat: (Jid) -> Unit,
    private val onNavigateToProfile: (UserId) -> Unit
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

    override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())

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

    override fun onContactSellerClicked(sellerEmail: Email) {
        val jid = Jid("${sellerEmail.value}@${SharedNetworkConstants.XMPP_DOMAIN}")
        onNavigateToChat(jid)
    }

    override fun onProfileClicked(userId: UserId) {
        onNavigateToProfile(userId)
    }
}