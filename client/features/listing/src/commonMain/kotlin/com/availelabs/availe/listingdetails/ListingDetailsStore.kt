@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.listingdetails

import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.listing_not_found
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.listingdetails.ListingDetailsStore.*
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IInteractionRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.RepositoryOperationResult
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface ListingDetailsStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object RetryRequested : Intent
        data object ToggleRsvp : Intent
        data object DismissLoginGuard : Intent
    }

    data class State(
        val listing: Listing? = null,
        val isLoading: Boolean = false,
        val errorMessage: UiText? = null,
        val currentUserId: UserId? = null,
        val showLoginGuard: Boolean = false
    )

    sealed interface Label {
        data class DisplayError(val error: AppError) : Label
    }
}

internal class ListingDetailsStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val interactionRepository: IInteractionRepository,
    private val listingId: ListingId
) {

    fun create(): ListingDetailsStore =
        object : ListingDetailsStore, Store<Intent, State, Label> by storeFactory.create(
            name = ListingDetailsStore::class.simpleName,
            initialState = State(),
            bootstrapper = ListingDetailsBootstrapper(listingId),
            executorFactory = { ListingDetailsExecutor() },
            reducer = ListingDetailsReducer
        ) {}

    private sealed interface Action {
        data class FetchListing(val listingId: ListingId) : Action
        data object FetchCurrentUser : Action
    }

    private sealed interface Message {
        data class SetLoading(val isLoading: Boolean) : Message
        data class ListingLoaded(val listing: Listing) : Message
        data class SetError(val message: UiText?) : Message
        data class SetCurrentUser(val userId: UserId?) : Message
        data class UpdateRsvpState(val isRsvped: Boolean) : Message
        data class SetShowLoginGuard(val showLoginGuard: Boolean) : Message
    }

    private class ListingDetailsBootstrapper(
        private val listingId: ListingId
    ) : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.FetchListing(listingId))
            dispatch(Action.FetchCurrentUser)
        }
    }

    private inner class ListingDetailsExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.FetchListing -> fetchListing(action.listingId)
                Action.FetchCurrentUser -> fetchCurrentUser()
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RetryRequested -> fetchListing(listingId)
                Intent.ToggleRsvp -> handleToggleRsvp()
                Intent.DismissLoginGuard -> dispatch(Message.SetShowLoginGuard(false))
            }
        }

        private fun fetchListing(id: ListingId) {
            dispatch(Message.SetLoading(true))
            dispatch(Message.SetError(null))

            scope.launch {
                when (val result = listingRepository.getListingById(id)) {
                    is RepositoryOperationResult.Success -> {
                        when (val findResult = result.value) {
                            is FindResult.Found -> {
                                dispatch(Message.ListingLoaded(findResult.value))
                            }

                            is FindResult.NotFound -> {
                                dispatch(Message.SetError(UiText.ResourceString(Res.string.listing_not_found)))
                            }
                        }
                    }

                    is RepositoryOperationResult.Failure -> {
                        val uiText = ErrorMessageMapper.map(result.error)
                        dispatch(Message.SetError(uiText))
                    }
                }

                dispatch(Message.SetLoading(false))
            }
        }

        private fun fetchCurrentUser() {
            scope.launch {
                val profile = sessionManager.getCurrentUserProfile()
                dispatch(Message.SetCurrentUser(profile?.id))
            }
        }

        private fun handleToggleRsvp() {
            val currentState = state()
            val userId = currentState.currentUserId
            if (userId == null) {
                dispatch(Message.SetShowLoginGuard(true))
                return
            }

            val currentListing = currentState.listing ?: return
            val currentRsvpState = currentListing.interactionState.isRsvped
            val newRsvpState = !currentRsvpState

            dispatch(Message.UpdateRsvpState(newRsvpState))

            scope.launch {
                val result = if (newRsvpState) {
                    interactionRepository.addInteraction(userId, currentListing.id, InteractionType.Rsvp)
                } else {
                    interactionRepository.removeInteraction(userId, currentListing.id, InteractionType.Rsvp)
                }

                when (result) {
                    is RepositoryOperationResult.Success -> {
                        dispatch(Message.ListingLoaded(result.value))
                    }

                    is RepositoryOperationResult.Failure -> {
                        dispatch(Message.UpdateRsvpState(currentRsvpState))
                        publish(Label.DisplayError(result.error))
                    }
                }
            }
        }
    }

    private object ListingDetailsReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.SetLoading -> copy(isLoading = msg.isLoading)
                is Message.ListingLoaded -> copy(listing = msg.listing)
                is Message.SetError -> copy(errorMessage = msg.message)
                is Message.SetCurrentUser -> copy(currentUserId = msg.userId)
                is Message.SetShowLoginGuard -> copy(showLoginGuard = msg.showLoginGuard)
                is Message.UpdateRsvpState -> {
                    val currentListing = listing
                    if (currentListing != null) {
                        val updatedInteractionState = currentListing.interactionState.copy(isRsvped = msg.isRsvped)
                        val updatedListing = when (currentListing) {
                            is Listing.Item -> currentListing.copy(interactionState = updatedInteractionState)
                            is Listing.Event -> currentListing.copy(interactionState = updatedInteractionState)
                            is Listing.Service -> currentListing.copy(interactionState = updatedInteractionState)
                        }
                        copy(listing = updatedListing)
                    } else {
                        this
                    }
                }
            }
    }
}