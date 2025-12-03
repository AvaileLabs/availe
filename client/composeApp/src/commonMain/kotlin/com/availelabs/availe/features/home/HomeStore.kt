@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, org.mobilenativefoundation.store.core5.ExperimentalStoreApi::class)

package com.availelabs.availe.features.home

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.data.repositories.IListingRepository
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.extensions.toDisplayString
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.RepositoryOperationResult
import com.availelabs.availe.features.home.HomeStore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.random.Random
import kotlin.time.Clock

data class ListingUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val price: String,
    val imageUrl: ImageUrl
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
        val errorMessage: String? = null,
        val isAuthenticated: Boolean = false,
        val currentPage: Int = 0,
        val endReached: Boolean = false
    )

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data class DisplayError(val message: String) : Label
    }
}

class HomeStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionManager
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
        data class ListingsError(val message: String) : Action
        data object ObserveAuthentication : Action
        data object LoadInitialPage : Action
    }

    private sealed interface Message {
        data class LoadingStateChanged(val loadingIndicator: LoadingIndicator, val isActive: Boolean) : Message
        data class AppendListings(val listingUiModels: List<ListingUiModel>, val page: Int) : Message
        data class ReplaceListings(val listingUiModels: List<ListingUiModel>) : Message
        data class SetError(val message: String?) : Message
        data class SetAuthenticated(val isAuthenticated: Boolean) : Message
        data class SetEndReached(val endReached: Boolean) : Message
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
                    dispatch(Message.SetError(action.message))
                    publish(Label.DisplayError(action.message))
                }

                Action.ObserveAuthentication -> observeAuthentication()
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
                    val errorMessage = "You must be logged in to create a listing"
                    dispatch(Message.SetError(errorMessage))
                    publish(Label.DisplayError(errorMessage))
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
                        publisherId = userProfile.id
                    )
                )

                when (val result = listingRepository.createListing(newListing)) {
                    is RepositoryOperationResult.Success<*> -> {
                        when (result.value) {
                            is InsertResult.Inserted<*> -> fetchListingsPage(0, FetchTrigger.DataChanged)
                            is InsertResult.AlreadyExists -> {
                                val errorMessage = "Listing already exists"
                                dispatch(Message.SetError(errorMessage))
                                publish(Label.DisplayError(errorMessage))
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
                        val errorMessage = result.error.value
                        dispatch(Message.SetError(errorMessage))
                        publish(Label.DisplayError(errorMessage))
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
            val startTimeMilliseconds = Clock.System.now().toEpochMilliseconds()
            val minimumRefreshDurationMilliseconds = 375L

            dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = true))
            dispatch(Message.SetError(null))

            if (page == 0) {
                dispatch(Message.SetEndReached(false))
            }

            scope.launch {
                val request = if (trigger == FetchTrigger.UserPullToRefresh || trigger == FetchTrigger.DataChanged) {
                    StoreReadRequest.fresh(ListingStoreKey(page, pageSize))
                } else {
                    StoreReadRequest.cached(ListingStoreKey(page, pageSize), refresh = false)
                }

                listingRepository.getListingsPageStream(request).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val data = response.requireData()
                            // Dispatch Action, not Message here. The reducer handles Messages.
                            // But since we are in the Executor scope, we can dispatch Messages directly if needed,
                            // or better, define an Action for loaded data if we want to process it.
                            // Here we use the Action defined earlier.
                            // We need to call `dispatch` which accepts `Message` inside `Reducer` or `Action` inside `Executor`?
                            // Wait, `dispatch` in `Executor` sends `Message` to `Reducer`.
                            // `Action` is input to `Executor`.
                            // So here we should create a Message.
                            // But previously I defined Action.ListingsLoaded.
                            // The logic was: `executeAction` handles `Action.ListingsLoaded`.
                            // But here we are IN `fetchListingsPage` which is called by `executeAction` or `executeIntent`.
                            // So we are already inside the processing logic. We should dispatch Messages.

                            val uiModels = data.map { it.toListingUiModel() }
                            if (page == 0) {
                                dispatch(Message.ReplaceListings(uiModels))
                            } else {
                                dispatch(Message.AppendListings(uiModels, page))
                            }
                            if (data.isEmpty()) {
                                dispatch(Message.SetEndReached(true))
                            }
                            dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
                        }

                        is StoreReadResponse.Error -> {
                            val errorMessage = response.errorMessageOrNull() ?: "Unknown error"
                            dispatch(Message.SetError(errorMessage))
                            publish(Label.DisplayError(errorMessage))
                            dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
                        }

                        is StoreReadResponse.Loading -> {
                            // Handled by initial dispatch
                        }

                        is StoreReadResponse.NoNewData -> {
                            dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
                        }

                        is StoreReadResponse.Initial -> {}
                    }
                }

                if (trigger is FetchTrigger.UserPullToRefresh) {
                    val elapsedTimeMilliseconds = Clock.System.now().toEpochMilliseconds() - startTimeMilliseconds
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
                val authenticationFlow = callbackFlow {
                    val subscription = sessionManager.authenticationState.subscribe { state ->
                        trySend(state)
                    }
                    awaitClose { subscription.cancel() }
                }

                authenticationFlow.collectLatest { authenticationState ->
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
    }

    private object HomeReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.LoadingStateChanged -> {
                    when (msg.loadingIndicator) {
                        LoadingIndicator.Fullscreen -> copy(isLoading = msg.isActive)
                        LoadingIndicator.Overlay -> copy(isRefreshing = msg.isActive)
                        LoadingIndicator.Pagination -> copy(isLoading = msg.isActive) // Or a specific pagination loading field
                    }
                }

                is Message.ReplaceListings -> copy(listingUiModels = msg.listingUiModels, currentPage = 0)
                is Message.AppendListings -> copy(
                    listingUiModels = listingUiModels + msg.listingUiModels,
                    currentPage = msg.page
                )

                is Message.SetError -> copy(errorMessage = msg.message)
                is Message.SetAuthenticated -> copy(isAuthenticated = msg.isAuthenticated)
                is Message.SetEndReached -> copy(endReached = msg.endReached)
            }
    }

    private fun Listing.toListingUiModel(): ListingUiModel {
        return ListingUiModel(
            id = this.id,
            title = this.title,
            price = this.price.toDisplayString(),
            imageUrl = this.imageLinks.firstOrNull() ?: ImageUrl("")
        )
    }
}