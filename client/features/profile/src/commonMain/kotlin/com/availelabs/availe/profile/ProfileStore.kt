package com.availelabs.availe.profile

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.profile.ProfileStore.*
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.repositories.IUserRepository
import com.availelabs.availe.types.PageSyncResult
import com.availelabs.availe.types.RepositoryOperationResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

data class ProfileListingUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val price: Price,
    val imageUrl: ImageUrl?,
    val category: ListingCategory,
    val description: ListingDescription?,
    val startTime: Instant?,
    val isRsvped: Boolean
)

interface ProfileStore : Store<Intent, State, Label> {
    sealed interface Intent {
        data object RefreshRequested : Intent
        data object LoadNextPage : Intent
        data class ListingClicked(val listingId: ListingId) : Intent
        data object BackClicked : Intent
        data object MessageClicked : Intent
        data object SettingsClicked : Intent
    }

    data class State(
        val targetUserId: UserId,
        val profile: PublicProfile? = null,
        val isCurrentUser: Boolean = false,
        override val databaseState: DatabaseState<ProfileListingUiModel> = DatabaseState.Uninitialized,
        override val networkSyncState: NetworkSyncState = NetworkSyncState.Idle,
        override val paginationState: PaginationState = PaginationState.Available,
        val paginationError: UiText? = null,
        val currentLimit: Long = 20L,
        val networkPage: Int = 0
    ) : IFeedUiState<ProfileListingUiModel>

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data object NavigateBack : Label
        data class NavigateToChat(val jid: Jid) : Label
        data object NavigateToSettings : Label
        data class ErrorOccurred(val error: AppError) : Label
    }
}

@OptIn(ExperimentalUuidApi::class)
internal class ProfileStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val userRepository: IUserRepository,
    private val sessionManager: ISessionRepository,
    private val targetUserId: UserId
) {
    fun create(): ProfileStore =
        object : ProfileStore, Store<Intent, State, Label> by storeFactory.create(
            name = ProfileStore::class.simpleName,
            initialState = State(targetUserId = targetUserId),
            bootstrapper = ProfileBootstrapper(),
            executorFactory = { ProfileExecutor() },
            reducer = ProfileReducer
        ) {}

    private sealed interface Action {
        data class SetIsCurrentUser(val isCurrentUser: Boolean) : Action
        data class ObserveDatabase(val limit: Long) : Action
        data class SyncNetwork(val page: Int, val trigger: SyncTrigger) : Action
        data object LoadProfile : Action
    }

    private sealed interface Message {
        data class SetIsCurrentUser(val isCurrentUser: Boolean) : Message
        data class SetDatabaseState(val databaseState: DatabaseState<ProfileListingUiModel>) : Message
        data class SetNetworkSyncState(val networkSyncState: NetworkSyncState) : Message
        data class SetPaginationState(val paginationState: PaginationState) : Message
        data class SetProfile(val profile: PublicProfile) : Message
        data class SetPaginationError(val error: UiText?) : Message
        data class SetNetworkPage(val page: Int) : Message
        data class SetCurrentLimit(val limit: Long) : Message
    }

    private inner class ProfileBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                val currentUserProfile = sessionManager.getCurrentUserProfile()
                val isMine = currentUserProfile?.id == targetUserId
                dispatch(Action.SetIsCurrentUser(isMine))
                dispatch(Action.ObserveDatabase(20L))
                dispatch(Action.SyncNetwork(page = 0, trigger = SyncTrigger.InitialLoad))
                dispatch(Action.LoadProfile)
            }
        }
    }

    private inner class ProfileExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        private val pageSize = 20
        private var databaseObservationJob: Job? = null

        override fun executeAction(action: Action) {
            when (action) {
                is Action.SetIsCurrentUser -> dispatch(Message.SetIsCurrentUser(action.isCurrentUser))
                is Action.ObserveDatabase -> observeDatabase(action.limit)
                is Action.SyncNetwork -> syncNetwork(action.page, action.trigger)
                is Action.LoadProfile -> fetchProfile()
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RefreshRequested -> {
                    dispatch(Message.SetCurrentLimit(20L))
                    dispatch(Message.SetPaginationState(PaginationState.Available))
                    executeAction(Action.ObserveDatabase(20L))
                    executeAction(Action.SyncNetwork(page = 0, trigger = SyncTrigger.PullToRefresh))
                    fetchProfile()
                }

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

                is Intent.ListingClicked -> publish(Label.NavigateToListingDetails(intent.listingId))
                Intent.BackClicked -> publish(Label.NavigateBack)
                Intent.MessageClicked -> {
                    val currentPublicProfile = state().profile
                    if (currentPublicProfile != null) {
                        publish(Label.NavigateToChat(currentPublicProfile.jid))
                    }
                }

                Intent.SettingsClicked -> publish(Label.NavigateToSettings)
            }
        }

        private fun fetchProfile() {
            scope.launch {
                when (val result = userRepository.getUserById(targetUserId)) {
                    is RepositoryOperationResult.Success -> dispatch(Message.SetProfile(result.value))
                    is RepositoryOperationResult.Failure -> {
                    }
                }
            }
        }

        private fun observeDatabase(limit: Long) {
            databaseObservationJob?.cancel()
            databaseObservationJob = scope.launch {
                listingRepository.observeFeed(
                    limit = limit,
                    publisherId = targetUserId
                ).collect { listings ->
                    val userInterfaceModels = listings.map { it.toProfileUiModel() }
                    val newDatabaseState = if (userInterfaceModels.isEmpty()) {
                        DatabaseState.Empty
                    } else {
                        DatabaseState.Populated(userInterfaceModels)
                    }
                    dispatch(Message.SetDatabaseState(newDatabaseState))
                }
            }
        }

        private fun syncNetwork(page: Int, trigger: SyncTrigger) {
            scope.launch {
                dispatch(Message.SetNetworkSyncState(NetworkSyncState.Active(trigger)))
                dispatch(Message.SetPaginationError(null))

                val synchronizationResult = listingRepository.syncFeedPage(
                    page = page,
                    size = pageSize,
                    publisherId = targetUserId
                )

                when (synchronizationResult) {
                    is PageSyncResult.Success -> {
                        dispatch(Message.SetNetworkPage(page))
                        dispatch(Message.SetNetworkSyncState(NetworkSyncState.Idle))
                        if (!synchronizationResult.hasMoreAvailable) {
                            dispatch(Message.SetPaginationState(PaginationState.EndReached))
                        } else if (page == 0) {
                            dispatch(Message.SetPaginationState(PaginationState.Available))
                        }
                    }

                    is PageSyncResult.Failure -> {
                        val userInterfaceTextError = ErrorMessageMapper.map(synchronizationResult.error)
                        dispatch(Message.SetNetworkSyncState(NetworkSyncState.Error(userInterfaceTextError, trigger)))

                        if (trigger is SyncTrigger.Pagination) {
                            dispatch(Message.SetPaginationError(userInterfaceTextError))
                        } else if (state().showListings) {
                            publish(Label.ErrorOccurred(synchronizationResult.error))
                        }
                    }
                }
            }
        }
    }

    private object ProfileReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State = when (msg) {
            is Message.SetIsCurrentUser -> copy(isCurrentUser = msg.isCurrentUser)
            is Message.SetDatabaseState -> copy(databaseState = msg.databaseState)
            is Message.SetNetworkSyncState -> copy(networkSyncState = msg.networkSyncState)
            is Message.SetPaginationState -> copy(paginationState = msg.paginationState)
            is Message.SetProfile -> copy(profile = msg.profile)
            is Message.SetPaginationError -> copy(paginationError = msg.error)
            is Message.SetNetworkPage -> copy(networkPage = msg.page)
            is Message.SetCurrentLimit -> copy(currentLimit = msg.limit)
        }
    }

    private fun Listing.toProfileUiModel(): ProfileListingUiModel {
        val listingCategory = when (this) {
            is Listing.Item -> ListingCategory.Item
            is Listing.Event -> ListingCategory.Event
            is Listing.Service -> ListingCategory.Service
        }
        val listingStartTimeInstant = (this as? Listing.Event)?.schedule?.startInstant

        return ProfileListingUiModel(
            id = this.id,
            title = this.title,
            price = this.price,
            imageUrl = this.imageLinks.firstOrNull(),
            category = listingCategory,
            description = this.description,
            startTime = listingStartTimeInstant,
            isRsvped = this.interactionState.isRsvped
        )
    }
}