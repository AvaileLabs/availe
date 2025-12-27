package com.availelabs.availe.features.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IProfileComponent {
    val state: StateFlow<ProfileStore.State>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ProfileStore.Intent)
    fun onBackClicked()
}

class ProfileComponent(
    componentContext: ComponentContext,
    private val targetUserId: UserId,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToListing: (ListingId) -> Unit,
    private val onNavigateToChat: (UserId) -> Unit,
    private val onNavigateToSettings: () -> Unit
) : IProfileComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ProfileStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            sessionManager = sessionManager,
            targetUserId = targetUserId
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ProfileStore.State> = store.stateFlow
    override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ProfileStore.Label.NavigateToListingDetails -> onNavigateToListing(label.listingId)
                    ProfileStore.Label.NavigateBack -> onNavigateBack()
                    ProfileStore.Label.NavigateToChat -> onNavigateToChat(targetUserId)
                    ProfileStore.Label.NavigateToSettings -> onNavigateToSettings()
                    is ProfileStore.Label.ErrorOccurred -> {
                        messageService.show(ToastMessage.Error(label.error))
                    }
                }
            }
        }
    }

    override fun onIntent(intent: ProfileStore.Intent) {
        store.accept(intent)
    }

    override fun onBackClicked() {
        store.accept(ProfileStore.Intent.BackClicked)
    }
}