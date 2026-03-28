package com.availelabs.availe.listingfeed

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.listingfeed.ListingFeedStore.*
import com.availelabs.availe.model.EventFeedQuery
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IEventRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.PageSyncResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
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
        override val databaseState: DatabaseState<ListingUiModel> = DatabaseState.Uninitialized,
        override val networkSyncState: NetworkSyncState = NetworkSyncState.Idle,
        override val paginationState: PaginationState = PaginationState.Available,
        val paginationError: UiText? = null,
        val isAuthenticated: Boolean = false,
        val currentLimit: Long = 20L,
        val networkPage: Int = 0,
        val agendaState: AgendaState = AgendaState.Hidden
    ) : IFeedUiState<ListingUiModel>

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data class DisplayError(val error: AppError) : Label
    }
}

internal class ListingFeedStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val eventRepository: IEventRepository,
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
        data class SetAgendaState(val agendaState: AgendaState) : Message
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
        private var mainFeedObservationJob: Job? = null
        private var agendaObservationJob: Job? = null

        private val tickingClockFlow: Flow<Instant> = flow {
            while (currentCoroutineContext().isActive) {
                emit(Clock.System.now())
                delay(60_000L)
            }
        }

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
                    if (currentState.canLoadMore) {
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
            mainFeedObservationJob?.cancel()
            agendaObservationJob?.cancel()

            mainFeedObservationJob = scope.launch {
                val eventFeedQuery = when (feedType) {
                    ListingFeedType.Upcoming -> EventFeedQuery.Upcoming(tickingClockFlow)
                    ListingFeedType.Going -> EventFeedQuery.Going
                    ListingFeedType.Past -> EventFeedQuery.Past(tickingClockFlow)
                }

                eventRepository.observeEventFeed(
                    limit = limit,
                    publisherId = null,
                    query = eventFeedQuery
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

            if (feedType is ListingFeedType.Upcoming) {
                agendaObservationJob = scope.launch {
                    val agendaFeedQuery = EventFeedQuery.Agenda(
                        tickingNow = tickingClockFlow,
                        cutoffDuration = 7.days
                    )

                    eventRepository.observeEventFeed(
                        limit = 15L,
                        publisherId = null,
                        query = agendaFeedQuery
                    ).collect { agendaListings ->
                        val currentInstant = Clock.System.now()
                        val agendaUiModels = agendaListings.map { eventListing ->
                            eventListing.toAgendaUiModel(currentInstant, eventListing.schedule.timeZoneIdentifier)
                        }

                        if (agendaUiModels.isEmpty()) {
                            dispatch(Message.SetAgendaState(AgendaState.Hidden))
                        } else {
                            dispatch(Message.SetAgendaState(AgendaState.Visible(agendaUiModels)))
                        }
                    }
                }
            } else {
                dispatch(Message.SetAgendaState(AgendaState.Hidden))
            }
        }

        private fun syncNetwork(page: Int, trigger: SyncTrigger) {
            scope.launch {
                dispatch(Message.SetNetworkSyncState(NetworkSyncState.Active(trigger)))
                dispatch(Message.SetPaginationError(null))

                val eventFeedQuery = when (feedType) {
                    ListingFeedType.Upcoming -> EventFeedQuery.Upcoming(tickingClockFlow)
                    ListingFeedType.Going -> EventFeedQuery.Going
                    ListingFeedType.Past -> EventFeedQuery.Past(tickingClockFlow)
                }

                val result = eventRepository.syncEventFeedPage(
                    page = page,
                    size = pageSize,
                    publisherId = null,
                    query = eventFeedQuery
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
                        } else if (state().showListings) {
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
                is Message.SetAgendaState -> copy(agendaState = msg.agendaState)
                Message.ResetState -> copy(
                    databaseState = DatabaseState.Uninitialized,
                    networkSyncState = NetworkSyncState.Idle,
                    paginationState = PaginationState.Available,
                    paginationError = null,
                    currentLimit = 20L,
                    networkPage = 0,
                    agendaState = AgendaState.Hidden
                )
            }
    }

    private fun Listing.toListingUiModel(): ListingUiModel {
        val category = when (this) {
            is Listing.Item -> ListingCategory.Item
            is Listing.Event -> ListingCategory.Event
            is Listing.Service -> ListingCategory.Service
        }
        val startTime = (this as? Listing.Event)?.schedule?.startInstant

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

    private fun Listing.Event.toAgendaUiModel(
        currentInstant: Instant,
        timeZoneIdentifier: TimeZoneIdentifier
    ): AgendaUiModel {
        val timeZone = TimeZone.of(timeZoneIdentifier.value)
        val currentLocalDate = currentInstant.toLocalDateTime(timeZone).date
        val startLocalDate = this.schedule.startInstant.toLocalDateTime(timeZone).date

        val proximity = if (currentInstant >= this.schedule.startInstant && currentInstant < this.schedule.endInstant) {
            EventProximity.Ongoing
        } else {
            val differenceInDays = (startLocalDate.toEpochDays() - currentLocalDate.toEpochDays()).toInt()
            when {
                differenceInDays <= 0 -> EventProximity.Today
                differenceInDays == 1 -> EventProximity.Tomorrow
                else -> EventProximity.Future(differenceInDays)
            }
        }

        return AgendaUiModel(
            id = this.id,
            title = this.title,
            imageUrl = this.imageLinks.firstOrNull(),
            startTime = this.schedule.startInstant,
            proximity = proximity
        )
    }
}