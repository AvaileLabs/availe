@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.availelabs.availe.features.home

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.data.repositories.ISessionManager
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.repository.IClientListingRepository
import com.availelabs.availe.domain.toDisplayString
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.RepositoryOperationResult
import com.availelabs.availe.features.home.HomeStore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    }

    data class State(
        val listingUiModels: List<ListingUiModel> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val isAuthenticated: Boolean = false
    )

    sealed interface Label {
        data class NavigateToListingDetails(val listingId: ListingId) : Label
        data class DisplayError(val message: String) : Label
    }
}

class HomeStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IClientListingRepository,
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
        data object FetchListings : Action
        data object ObserveAuthentication : Action
    }

    private sealed interface Message {
        data class LoadingStateChanged(val loadingIndicator: LoadingIndicator, val isActive: Boolean) : Message
        data class ListingsLoaded(val listingUiModels: List<ListingUiModel>) : Message
        data class SetError(val message: String?) : Message
        data class SetAuthenticated(val isAuthenticated: Boolean) : Message
    }

    private sealed interface FetchTrigger {
        data object InitialLoad : FetchTrigger
        data object UserPullToRefresh : FetchTrigger
        data object DataChanged : FetchTrigger
    }

    private sealed interface LoadingIndicator {
        data object Fullscreen : LoadingIndicator
        data object Overlay : LoadingIndicator
    }

    private class HomeBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.ObserveAuthentication)
            dispatch(Action.FetchListings)
        }
    }

    private inner class HomeExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                Action.FetchListings -> fetchListings(FetchTrigger.InitialLoad)
                Action.ObserveAuthentication -> observeAuthentication()
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RefreshListingsRequested -> fetchListings(FetchTrigger.UserPullToRefresh)
                Intent.QuickCreateListingRequested -> performQuickCreate()
                is Intent.ListingClicked -> publish(Label.NavigateToListingDetails(intent.listingId))
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
                    is RepositoryOperationResult.Success -> {
                        when (result.value) {
                            is InsertResult.Inserted -> fetchListings(FetchTrigger.DataChanged)
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

        private fun fetchListings(trigger: FetchTrigger) {
            val currentListingUiModels = state().listingUiModels
            val loadingIndicator = resolveLoadingIndicator(trigger, currentListingUiModels)
            val startTimeMilliseconds = Clock.System.now().toEpochMilliseconds()
            val minimumRefreshDurationMilliseconds = 750L

            dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = true))
            dispatch(Message.SetError(null))

            scope.launch {
                val result = listingRepository.getAllListings()

                when (result) {
                    is RepositoryOperationResult.Success -> {
                        val listingUiModels = result.value.map { it.toListingUiModel() }
                        dispatch(Message.ListingsLoaded(listingUiModels))
                    }

                    is RepositoryOperationResult.Failure -> {
                        val errorMessage = result.error.value
                        dispatch(Message.SetError(errorMessage))
                        publish(Label.DisplayError(errorMessage))
                    }
                }

                if (trigger is FetchTrigger.UserPullToRefresh) {
                    val elapsedTimeMilliseconds = Clock.System.now().toEpochMilliseconds() - startTimeMilliseconds
                    if (elapsedTimeMilliseconds < minimumRefreshDurationMilliseconds) {
                        delay(minimumRefreshDurationMilliseconds - elapsedTimeMilliseconds)
                    }
                }

                dispatch(Message.LoadingStateChanged(loadingIndicator, isActive = false))
            }
        }

        private fun resolveLoadingIndicator(
            trigger: FetchTrigger,
            currentListingUiModels: List<ListingUiModel>
        ): LoadingIndicator {
            return if (currentListingUiModels.isEmpty()) {
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
                    }
                }

                is Message.ListingsLoaded -> copy(listingUiModels = msg.listingUiModels)
                is Message.SetError -> copy(errorMessage = msg.message)
                is Message.SetAuthenticated -> copy(isAuthenticated = msg.isAuthenticated)
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