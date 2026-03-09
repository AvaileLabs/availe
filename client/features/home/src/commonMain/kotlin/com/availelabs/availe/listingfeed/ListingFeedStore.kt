package com.availelabs.availe.listingfeed

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.listingfeed.ListingFeedStore.*
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.model.toEventFeedFilter
import com.availelabs.availe.model.toListingCategoryFilter
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.PageSyncResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class ListingUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val price: Price,
    val imageUrl: ImageUrl?,
    val category: ListingCategory,
    val description: ListingDescription?,
    val startTime: Instant?,
    val isRsvped: Boolean
)

interface ListingFeedStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object RefreshListingsRequested : Intent
        data class ListingClicked(val listingId: ListingId) : Intent
        data object LoadNextPage : Intent
    }

    data class State(
        val databaseState: DatabaseState<ListingUiModel> = DatabaseState.Uninitialized,
        val networkSyncState: NetworkSyncState = NetworkSyncState.Idle,
        val paginationState: PaginationState = PaginationState.Available,
        val paginationError: UiText? = null,
        val isAuthenticated: Boolean = false,
        val currentLimit: Long = 20L,
        val networkPage: Int = 0
    )

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data class DisplayError(val error: AppError) : Label
    }
}

internal class ListingFeedStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val feedType: ListingFeedType
) {

    fun create(): ListingFeedStore =
        object : ListingFeedStore, Store<Intent, State, Label> by storeFactory.create(
            name = "${ListingFeedStore::class.simpleName}_$feedType",
            initialState = State(),
            bootstrapper = ListingFeedBootstrapper(),
            executorFactory = { ListingFeedExecutor() },
            reducer = ListingFeedReducer
        ) {}

    private sealed interface Action {
        data class ObserveDatabase(val limit: Long) : Action
        data class SyncNetwork(val page: Int, val trigger: SyncTrigger) : Action
        data object ObserveAuthentication : Action
        data object ObserveSessionInvalidation : Action
    }

    private sealed interface Message {
        data class SetDatabaseState(val databaseState: DatabaseState<ListingUiModel>) : Message
        data class SetNetworkSyncState(val networkSyncState: NetworkSyncState) : Message
        data class SetPaginationState(val paginationState: PaginationState) : Message
        data class SetPaginationError(val error: UiText?) : Message
        data class SetAuthenticated(val isAuthenticated: Boolean) : Message
        data class SetNetworkPage(val page: Int) : Message
        data class SetCurrentLimit(val limit: Long) : Message
        data object ResetState : Message
    }

    private class ListingFeedBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.ObserveAuthentication)
            dispatch(Action.ObserveSessionInvalidation)
            dispatch(Action.ObserveDatabase(20L))
        }
    }

    private inner class ListingFeedExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        private val pageSize = 20
        private var databaseObservationJob: Job? = null

        override fun executeAction(action: Action) {
            when (action) {
                is Action.ObserveDatabase -> observeDatabase(action.limit)
                is Action.SyncNetwork -> syncNetwork(action.page, action.trigger)
                Action.ObserveAuthentication -> observeAuthentication()
                Action.ObserveSessionInvalidation -> observeSessionInvalidation()
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RefreshListingsRequested -> {
                    dispatch(Message.SetCurrentLimit(20L))
                    dispatch(Message.SetPaginationState(PaginationState.Available))
                    executeAction(Action.ObserveDatabase(20L))
                    executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.PullToRefresh))
                }

                is Intent.ListingClicked -> publish(Label.NavigateToListingDetails(intent.listingId))
                Intent.LoadNextPage -> {
                    val currentState = state()
                    if (currentState.paginationState is PaginationState.Available && currentState.networkSyncState is NetworkSyncState.Idle && currentState.paginationError == null) {
                        val newLimit = currentState.currentLimit + 20L
                        val nextPage = currentState.networkPage + 1
                        dispatch(Message.SetCurrentLimit(newLimit))
                        executeAction(Action.ObserveDatabase(newLimit))
                        executeAction(Action.SyncNetwork(page = nextPage, trigger = SyncTrigger.Pagination))
                    }
                }
            }
        }

        private fun observeDatabase(limit: Long) {
            databaseObservationJob?.cancel()
            databaseObservationJob = scope.launch {
                listingRepository.observeFeed(
                    limit = limit,
                    publisherId = null,
                    filter = feedType.toListingCategoryFilter(),
                    eventFeedFilter = feedType.toEventFeedFilter()
                ).collect { listings ->
                    val uiModels = listings.map { it.toListingUiModel() }
                    val newDatabaseState = if (uiModels.isEmpty()) {
                        DatabaseState.Empty
                    } else {
                        DatabaseState.Populated(uiModels)
                    }
                    dispatch(Message.SetDatabaseState(newDatabaseState))
                }
            }
        }

        private fun syncNetwork(page: Int, trigger: SyncTrigger) {
            scope.launch {
                dispatch(Message.SetNetworkSyncState(NetworkSyncState.Active(trigger)))
                dispatch(Message.SetPaginationError(null))

                val result = listingRepository.syncFeedPage(
                    page = page,
                    size = pageSize,
                    publisherId = null,
                    filter = feedType.toListingCategoryFilter(),
                    eventFeedFilter = feedType.toEventFeedFilter()
                )

                when (result) {
                    is PageSyncResult.Success -> {
                        dispatch(Message.SetNetworkPage(page))
                        dispatch(Message.SetNetworkSyncState(NetworkSyncState.Idle))
                        if (!result.hasMoreAvailable) {
                            dispatch(Message.SetPaginationState(PaginationState.EndReached))
                        } else if (page == 0) {
                            dispatch(Message.SetPaginationState(PaginationState.Available))
                        }
                    }

                    is PageSyncResult.Failure -> {
                        val uiText = ErrorMessageMapper.map(result.error)
                        dispatch(Message.SetNetworkSyncState(NetworkSyncState.Error(uiText, trigger)))

                        if (trigger is SyncTrigger.Pagination) {
                            dispatch(Message.SetPaginationError(uiText))
                        } else if (state().databaseState is DatabaseState.Populated) {
                            publish(Label.DisplayError(result.error))
                        }
                    }
                }
            }
        }

        private fun observeAuthentication() {
            scope.launch {
                var isInitialStateResolved = false

                sessionManager.authenticationState.collectLatest { authenticationState ->
                    if (authenticationState == AuthenticationState.Unknown) {
                        return@collectLatest
                    }

                    val wasAuthenticated = state().isAuthenticated

                    when (authenticationState) {
                        is AuthenticationState.LoggedIn -> {
                            dispatch(Message.SetAuthenticated(true))
                            if (!isInitialStateResolved) {
                                isInitialStateResolved = true
                                executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.InitialLoad))
                            } else if (!wasAuthenticated) {
                                dispatch(Message.ResetState)
                                executeAction(Action.ObserveDatabase(20L))
                                executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.InitialLoad))
                            }
                        }

                        is AuthenticationState.LoggedOut,
                        is AuthenticationState.SessionExpired,
                        is AuthenticationState.InitializationFailed -> {
                            dispatch(Message.SetAuthenticated(false))
                            if (!isInitialStateResolved) {
                                isInitialStateResolved = true
                                executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.InitialLoad))
                            } else if (wasAuthenticated) {
                                dispatch(Message.ResetState)
                                executeAction(Action.ObserveDatabase(20L))
                                executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.InitialLoad))
                            }
                        }

                        AuthenticationState.Unknown -> {}
                    }
                }
            }
        }

        private fun observeSessionInvalidation() {
            scope.launch {
                sessionManager.sessionInvalidated.collect {
                    dispatch(Message.ResetState)
                }
            }
        }
    }

    private object ListingFeedReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.SetDatabaseState -> copy(databaseState = msg.databaseState)
                is Message.SetNetworkSyncState -> copy(networkSyncState = msg.networkSyncState)
                is Message.SetPaginationState -> copy(paginationState = msg.paginationState)
                is Message.SetPaginationError -> copy(paginationError = msg.error)
                is Message.SetAuthenticated -> copy(isAuthenticated = msg.isAuthenticated)
                is Message.SetNetworkPage -> copy(networkPage = msg.page)
                is Message.SetCurrentLimit -> copy(currentLimit = msg.limit)
                Message.ResetState -> copy(
                    databaseState = DatabaseState.Uninitialized,
                    networkSyncState = NetworkSyncState.Idle,
                    paginationState = PaginationState.Available,
                    paginationError = null,
                    currentLimit = 20L,
                    networkPage = 0
                )
            }
    }

    private fun Listing.toListingUiModel(): ListingUiModel {
        val category = when (this) {
            is Listing.Item -> ListingCategory.Item
            is Listing.Event -> ListingCategory.Event
            is Listing.Service -> ListingCategory.Service
        }
        val startTime = (this as? Listing.Event)?.startTime

        return ListingUiModel(
            id = this.id,
            title = this.title,
            price = this.price,
            imageUrl = this.imageLinks.firstOrNull(),
            category = category,
            description = this.description,
            startTime = startTime,
            isRsvped = this.interactionState.isRsvped
        )
    }
}