package com.availelabs.availe.features.home

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.features.home.HomeStore.*
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.RepositoryOperationResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock.System
import kotlin.uuid.ExperimentalUuidApi

data class ListingUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val price: Price,
    val imageUrl: com.availelabs.availe.domain.model.ImageUrl?,
    val category: ListingCategory,
    val description: ListingDescription?
)

interface HomeStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object RefreshListingsRequested : Intent
        data object QuickCreateListingRequested : Intent
        data class ListingClicked(val listingId: ListingId) : Intent
        data object LoadNextPage : Intent
    }

    data class State(
        val listingUiModels: List<ListingUiModel> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val inlineError: UiText? = null,
        val paginationError: UiText? = null,
        val isAuthenticated: Boolean = false,
        val currentPage: Int = 0,
        val endReached: Boolean = false,
        val hasLoadedSuccessfully: Boolean = false
    )

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data class DisplayError(val error: AppError) : Label
    }
}

@OptIn(ExperimentalUuidApi::class)
class HomeStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository
) {

    fun create(): HomeStore =
        object : HomeStore, Store<Intent, State, Label> by storeFactory.create(
            name = HomeStore::class.simpleName,
            initialState = State(),
            bootstrapper = HomeBootstrapper(),
            executorFactory = { HomeExecutor() },
            reducer = HomeReducer
        ) {}

    private sealed interface Action {
        data class ListingsLoaded(val listings: List<Listing>, val page: Int) : Action
        data class ListingsError(val error: AppError, val isPagination: Boolean) : Action
        data object ObserveAuthentication : Action
        data object ObserveSessionInvalidation : Action
        data object DataPurged : Action
        data object LoadInitialPage : Action
    }

    private sealed interface Message {
        data class LoadingStateChanged(val loadingIndicator: LoadingIndicator, val isActive: Boolean) : Message
        data class AppendListings(val listingUiModels: List<ListingUiModel>, val page: Int) : Message
        data class ReplaceListings(val listingUiModels: List<ListingUiModel>) : Message
        data class SetInlineError(val message: UiText?) : Message
        data class SetPaginationError(val message: UiText?) : Message
        data class SetAuthenticated(val isAuthenticated: Boolean) : Message
        data class SetEndReached(val endReached: Boolean) : Message
        data object ResetState : Message
    }

    private sealed interface FetchTrigger {
        data object InitialLoad : FetchTrigger
        data object UserPullToRefresh : FetchTrigger
        data object LoadMore : FetchTrigger
        data object DataChanged : FetchTrigger
    }

    private sealed interface LoadingIndicator {
        data object Fullscreen : LoadingIndicator
        data object Overlay : LoadingIndicator
        data object Pagination : LoadingIndicator
    }

    private class HomeBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.ObserveAuthentication)
            dispatch(Action.ObserveSessionInvalidation)
            dispatch(Action.LoadInitialPage)
        }
    }

    private inner class HomeExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        private val pageSize = 20

        override fun executeAction(action: Action) {
            when (action) {
                is Action.ListingsLoaded -> {
                    val uiModels = action.listings.map { it.toListingUiModel() }

                    if (action.page == 0) {
                        dispatch(Message.ReplaceListings(uiModels))
                    } else {
                        dispatch(Message.AppendListings(uiModels, action.page))
                    }

                    if (action.listings.isEmpty()) {
                        dispatch(Message.SetEndReached(true))
                    }
                }

                is Action.ListingsError -> {
                    val uiText = ErrorMessageMapper.map(action.error)
                    if (action.isPagination) {
                        dispatch(Message.SetPaginationError(uiText))
                    } else if (state().listingUiModels.isEmpty()) {
                        dispatch(Message.SetInlineError(uiText))
                    } else {
                        publish(Label.DisplayError(action.error))
                    }
                }

                Action.ObserveAuthentication -> observeAuthentication()
                Action.ObserveSessionInvalidation -> observeSessionInvalidation()
                Action.DataPurged -> dispatch(Message.ResetState)
                Action.LoadInitialPage -> fetchListingsPage(0, FetchTrigger.InitialLoad)
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RefreshListingsRequested -> fetchListingsPage(0, FetchTrigger.UserPullToRefresh)
                Intent.QuickCreateListingRequested -> performQuickCreate()
                is Intent.ListingClicked -> publish(Label.NavigateToListingDetails(intent.listingId))
                Intent.LoadNextPage -> {
                    val state = state()
                    if (!state.isLoading && !state.endReached) {
                        fetchListingsPage(state.currentPage + 1, FetchTrigger.LoadMore)
                    }
                }
            }
        }

        private fun performQuickCreate() {
            scope.launch {
                val userProfile = sessionManager.getCurrentUserProfile()
                if (userProfile == null) {
                    val error = AppError.BusinessError.OperationForbidden
                    publish(Label.DisplayError(error))
                    return@launch
                }

                dispatch(Message.LoadingStateChanged(loadingIndicator = LoadingIndicator.Overlay, isActive = true))

                val randomId = Random.nextInt(1000, 9999)
                val newListing = NewListing.Item(
                    commonData = CommonListingData(
                        title = ListingTitle("Quick Item $randomId"),
                        description = ListingDescription("This item was created via quick add action."),
                        price = Price(PriceAmount("10.00"), SupportedCurrency.USD.code),
                        imageLinks = emptyList(),
                        publisherId = userProfile.id,
                        publisherEmail = userProfile.email
                    )
                )

                when (val result = listingRepository.createListing(newListing)) {
                    is RepositoryOperationResult.Success<*> -> {
                        when (result.value) {
                            is InsertResult.Inserted<*> -> fetchListingsPage(0, FetchTrigger.DataChanged)
                            is InsertResult.AlreadyExists -> {
                                val error = AppError.BusinessError.OperationForbidden
                                publish(Label.DisplayError(error))
                                dispatch(
                                    Message.LoadingStateChanged(
                                        loadingIndicator = LoadingIndicator.Overlay,
                                        isActive = false
                                    )
                                )
                            }
                        }
                    }

                    is RepositoryOperationResult.Failure -> {
                        publish(Label.DisplayError(result.error))
                        dispatch(
                            Message.LoadingStateChanged(
                                loadingIndicator = LoadingIndicator.Overlay,
                                isActive = false
                            )
                        )
                    }
                }
            }
        }

        private fun fetchListingsPage(page: Int, trigger: FetchTrigger) {
            val currentListingUiModels = state().listingUiModels
            val loadingIndicator = resolveLoadingIndicator(trigger, currentListingUiModels)
            val startTimeMilliseconds = System.now().toEpochMilliseconds()
            val minimumRefreshDurationMilliseconds = 375L

            // We delegate state management to the repository stream.
            // This ensures the UI state (Loading/Data/Error) stays synchronized with
            // internal repository events, such as automatic retries or cache refreshes.

            scope.launch {
                val forceRefresh = trigger == FetchTrigger.UserPullToRefresh || trigger == FetchTrigger.DataChanged

                listingRepository.getListingsPageStream(page, pageSize, forceRefresh)
                    .catch { throwable ->
                        val appError = AppError.UnknownError(throwable.message ?: "Unknown stream error")
                        forward(Action.ListingsError(appError, isPagination = trigger == FetchTrigger.LoadMore))
                        dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
                    }
                    .collect { streamResult ->
                        when (streamResult) {
                            is DataStream.Loading -> {
                                dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = true))
                            }

                            is DataStream.Data -> {
                                forward(Action.ListingsLoaded(streamResult.value, page))
                            }

                            is DataStream.Error -> {
                                forward(
                                    Action.ListingsError(
                                        streamResult.error,
                                        isPagination = trigger == FetchTrigger.LoadMore
                                    )
                                )
                                dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
                            }
                        }
                    }

                if (trigger is FetchTrigger.UserPullToRefresh) {
                    val elapsedTimeMilliseconds = System.now().toEpochMilliseconds() - startTimeMilliseconds
                    if (elapsedTimeMilliseconds < minimumRefreshDurationMilliseconds) {
                        delay(minimumRefreshDurationMilliseconds - elapsedTimeMilliseconds)
                    }
                }
            }
        }

        private fun resolveLoadingIndicator(
            trigger: FetchTrigger,
            currentListingUiModels: List<ListingUiModel>
        ): LoadingIndicator {
            return if (trigger == FetchTrigger.LoadMore) {
                LoadingIndicator.Pagination
            } else if (currentListingUiModels.isEmpty()) {
                LoadingIndicator.Fullscreen
            } else {
                LoadingIndicator.Overlay
            }
        }

        private fun observeAuthentication() {
            scope.launch {
                sessionManager.authenticationState.collectLatest { authenticationState ->
                    when (authenticationState) {
                        is AuthenticationState.LoggedIn -> {
                            dispatch(Message.SetAuthenticated(true))
                        }

                        is AuthenticationState.LoggedOut -> {
                            dispatch(Message.SetAuthenticated(false))
                        }

                        is AuthenticationState.SessionExpired -> {
                            dispatch(Message.SetAuthenticated(false))
                        }

                        is AuthenticationState.InitializationFailed -> {
                            dispatch(Message.SetAuthenticated(false))
                        }

                        AuthenticationState.Unknown -> {
                        }
                    }
                }
            }
        }

        private fun observeSessionInvalidation() {
            scope.launch {
                sessionManager.sessionInvalidated.collect {
                    forward(Action.DataPurged)
                }
            }
        }
    }

    private object HomeReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.LoadingStateChanged -> {
                    when (msg.loadingIndicator) {
                        LoadingIndicator.Fullscreen -> copy(isLoading = msg.isActive)
                        LoadingIndicator.Overlay -> copy(isRefreshing = msg.isActive)
                        LoadingIndicator.Pagination -> copy(isLoading = msg.isActive)
                    }
                }

                is Message.ReplaceListings -> copy(
                    listingUiModels = msg.listingUiModels,
                    currentPage = 0,
                    isLoading = false,
                    isRefreshing = false,
                    inlineError = null,
                    paginationError = null,
                    hasLoadedSuccessfully = true
                )

                is Message.AppendListings -> copy(
                    listingUiModels = listingUiModels + msg.listingUiModels,
                    currentPage = msg.page,
                    isLoading = false,
                    isRefreshing = false,
                    paginationError = null,
                    hasLoadedSuccessfully = true
                )

                is Message.SetInlineError -> copy(
                    inlineError = msg.message,
                    isLoading = false,
                    isRefreshing = false
                )

                is Message.SetPaginationError -> copy(
                    paginationError = msg.message,
                    isLoading = false,
                    isRefreshing = false
                )

                is Message.SetAuthenticated -> copy(isAuthenticated = msg.isAuthenticated)
                is Message.SetEndReached -> copy(endReached = msg.endReached)
                Message.ResetState -> copy(
                    listingUiModels = emptyList(),
                    currentPage = 0,
                    endReached = false,
                    isLoading = false,
                    isRefreshing = false,
                    inlineError = null,
                    paginationError = null,
                    hasLoadedSuccessfully = false
                )
            }
    }

    private fun Listing.toListingUiModel(): ListingUiModel {
        val category = when (this) {
            is Listing.Item -> ListingCategory.Item
            is Listing.Event -> ListingCategory.Event
            is Listing.Service -> ListingCategory.Service
        }
        return ListingUiModel(
            id = this.id,
            title = this.title,
            price = this.price,
            imageUrl = this.imageLinks.firstOrNull(),
            category = category,
            description = this.description
        )
    }
}