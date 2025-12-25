package com.availelabs.availe.features.home

import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.accessibility_action_add
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionManager
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.ActionIcon
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IHomeComponent {
    val state: StateFlow<HomeStore.State>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: HomeStore.Intent)
}

class HomeComponent(
    componentContext: ComponentContext,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionManager,
    private val onNavigateToListingDetails: (ListingId) -> Unit,
    private val messageService: IMessageService
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

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is HomeStore.Label.NavigateToListingDetails -> onNavigateToListingDetails(label.listingId)
                    is HomeStore.Label.DisplayError -> {
                        messageService.show(ToastMessage.Error(label.error))
                    }
                }
            }
        }

        val authenticationSubscription = sessionManager.authenticationState.subscribe { authenticationState ->
            if (authenticationState is AuthenticationState.LoggedIn) {
                _actions.value = listOf(
                    AppBarAction(
                        icon = ActionIcon.ADD,
                        label = UiText.ResourceString(Res.string.accessibility_action_add),
                        onClick = { store.accept(HomeStore.Intent.QuickCreateListingRequested) }
                    )
                )
            } else {
                _actions.value = emptyList()
            }
        }
        lifecycle.doOnDestroy { authenticationSubscription.cancel() }
    }

    override fun onIntent(intent: HomeStore.Intent) {
        store.accept(intent)
    }
}