package com.availelabs.availe.profile

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.profile.ProfileStore.*
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.repositories.IUserRepository
import com.availelabs.availe.types.RepositoryOperationResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Clock.System
import kotlin.uuid.ExperimentalUuidApi

data class ProfileListingUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val price: Price,
    val imageUrl: ImageUrl?,
    val category: ListingCategory,
    val description: ListingDescription?
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
        val listings: List<ProfileListingUiModel> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: UiText? = null,
        val paginationError: UiText? = null,
        val currentPage: Int = 0,
        val endReached: Boolean = false
    )

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data object NavigateBack : Label
        data object NavigateToChat : Label
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
        data class ListingsLoaded(val listings: List<Listing>, val page: Int) : Action
        data class ListingsError(val error: AppError, val isPagination: Boolean) : Action
        data object LoadInitialPage : Action
        data object LoadProfile : Action
    }

    private sealed interface Message {
        data class SetIsCurrentUser(val isCurrentUser: Boolean) : Message
        data class SetLoading(val isLoading: Boolean) : Message
        data class SetRefreshing(val isRefreshing: Boolean) : Message
        data class AppendListings(val listings: List<ProfileListingUiModel>, val page: Int) : Message
        data class ReplaceListings(val listings: List<ProfileListingUiModel>) : Message
        data class SetProfile(val profile: PublicProfile) : Message
        data class SetError(val error: UiText?) : Message
        data class SetPaginationError(val error: UiText?) : Message
        data class SetEndReached(val endReached: Boolean) : Message
    }

    private inner class ProfileBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                val currentUserProfile = sessionManager.getCurrentUserProfile()
                val isMine = currentUserProfile?.id == targetUserId
                dispatch(Action.SetIsCurrentUser(isMine))
                dispatch(Action.LoadInitialPage)
                dispatch(Action.LoadProfile)
            }
        }
    }

    private inner class ProfileExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        private val pageSize = 20

        override fun executeAction(action: Action) {
            when (action) {
                is Action.SetIsCurrentUser -> dispatch(Message.SetIsCurrentUser(action.isCurrentUser))
                is Action.LoadInitialPage -> fetchListings(0, isRefresh = false)
                is Action.LoadProfile -> fetchProfile()
                is Action.ListingsLoaded -> handleListingsLoaded(action.listings, action.page)
                is Action.ListingsError -> handleListingsError(action.error, action.isPagination)
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RefreshRequested -> {
                    fetchListings(0, isRefresh = true)
                    fetchProfile()
                }

                Intent.LoadNextPage -> {
                    val state = state()
                    if (!state.isLoading && !state.endReached) {
                        fetchListings(state.currentPage + 1, isRefresh = false)
                    }
                }

                is Intent.ListingClicked -> publish(Label.NavigateToListingDetails(intent.listingId))
                Intent.BackClicked -> publish(Label.NavigateBack)
                Intent.MessageClicked -> publish(Label.NavigateToChat)
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

        private fun fetchListings(page: Int, isRefresh: Boolean) {
            val startTime = System.now().toEpochMilliseconds()
            val minimumRefreshDuration = 500L

            scope.launch {
                listingRepository.getListingsPageStream(
                    page = page,
                    size = pageSize,
                    forceRefresh = isRefresh,
                    publisherId = targetUserId
                )
                    .catch { throwable ->
                        val appError = AppError.UnknownError(throwable.message ?: "Unknown error")
                        forward(Action.ListingsError(appError, isPagination = page > 0))
                        updateLoadingState(isActive = false, isRefresh = isRefresh)
                    }
                    .collect { streamResult ->
                        when (streamResult) {
                            DataStream.Loading -> {
                                updateLoadingState(isActive = true, isRefresh = isRefresh)
                            }

                            is DataStream.Data -> {
                                if (isRefresh) {
                                    val elapsedTime = System.now().toEpochMilliseconds() - startTime
                                    if (elapsedTime < minimumRefreshDuration) {
                                        delay(minimumRefreshDuration - elapsedTime)
                                    }
                                }
                                forward(Action.ListingsLoaded(streamResult.value, page))
                                updateLoadingState(isActive = false, isRefresh = isRefresh)
                            }

                            is DataStream.Error -> {
                                forward(Action.ListingsError(streamResult.error, isPagination = page > 0))
                                updateLoadingState(isActive = false, isRefresh = isRefresh)
                            }
                        }
                    }
            }
        }

        private fun updateLoadingState(isActive: Boolean, isRefresh: Boolean) {
            if (isRefresh) {
                dispatch(Message.SetRefreshing(isActive))
            } else {
                dispatch(Message.SetLoading(isActive))
            }
        }

        private fun handleListingsLoaded(listings: List<Listing>, page: Int) {
            val uiModels = listings.map { it.toProfileUiModel() }
            if (page == 0) {
                dispatch(Message.ReplaceListings(uiModels))
            } else {
                dispatch(Message.AppendListings(uiModels, page))
            }
            if (listings.isEmpty()) {
                dispatch(Message.SetEndReached(true))
            }
        }

        private fun handleListingsError(error: AppError, isPagination: Boolean) {
            val uiText = ErrorMessageMapper.map(error)
            if (isPagination) {
                dispatch(Message.SetPaginationError(uiText))
            } else {
                if (state().listings.isEmpty()) {
                    dispatch(Message.SetError(uiText))
                } else {
                    publish(Label.ErrorOccurred(error))
                }
            }
        }
    }

    private object ProfileReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State = when (msg) {
            is Message.SetIsCurrentUser -> copy(isCurrentUser = msg.isCurrentUser)
            is Message.SetLoading -> copy(isLoading = msg.isLoading)
            is Message.SetRefreshing -> copy(isRefreshing = msg.isRefreshing)
            is Message.SetProfile -> copy(profile = msg.profile)
            is Message.ReplaceListings -> copy(
                listings = msg.listings,
                currentPage = 0,
                endReached = false,
                error = null,
                paginationError = null
            )

            is Message.AppendListings -> copy(
                listings = listings + msg.listings,
                currentPage = msg.page,
                paginationError = null
            )

            is Message.SetError -> copy(error = msg.error)
            is Message.SetPaginationError -> copy(paginationError = msg.error)
            is Message.SetEndReached -> copy(endReached = msg.endReached)
        }
    }

    private fun Listing.toProfileUiModel(): ProfileListingUiModel {
        val category = when (this) {
            is Listing.Item -> ListingCategory.Item
            is Listing.Event -> ListingCategory.Event
            is Listing.Service -> ListingCategory.Service
        }
        return ProfileListingUiModel(
            id = this.id,
            title = this.title,
            price = this.price,
            imageUrl = this.imageLinks.firstOrNull(),
            category = category,
            description = this.description
        )
    }
}