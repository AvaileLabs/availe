package com.availelabs.availe.listingeditor

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.*
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.launch

interface IListingEditorStore : Store<ListingEditorIntent, ListingEditorState, ListingEditorLabel>

sealed interface ListingEditorLabel {
    data object ListingCreated : ListingEditorLabel
    data object ListingUpdated : ListingEditorLabel
    data class ErrorOccurred(val error: AppError) : ListingEditorLabel
}

internal class ListingEditorStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val listingId: ListingId?
) {

    fun create(): IListingEditorStore =
        object : IListingEditorStore,
            Store<ListingEditorIntent, ListingEditorState, ListingEditorLabel> by storeFactory.create(
                name = IListingEditorStore::class.simpleName,
                initialState = ListingEditorState(),
                bootstrapper = ListingEditorBootstrapper(),
                executorFactory = { ListingEditorExecutor() },
                reducer = ListingEditorReducer
            ) {}

    private sealed interface Action {
        data class PopulateListing(val listing: Listing) : Action
    }

    private inner class ListingEditorBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            if (listingId != null) {
                scope.launch {
                    when (val result = listingRepository.getListingById(listingId)) {
                        is RepositoryOperationResult.Success -> {
                            if (result.value is FindResult.Found) {
                                val listing = (result.value as FindResult.Found).value
                                dispatch(Action.PopulateListing(listing))
                            }
                        }

                        is RepositoryOperationResult.Failure -> {
                        }
                    }
                }
            }
        }
    }

    private inner class ListingEditorExecutor :
        CoroutineExecutor<ListingEditorIntent, Action, ListingEditorState, ListingEditorIntent, ListingEditorLabel>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.PopulateListing -> {
                    dispatch(ListingEditorIntent.LoadData(action.listing))
                }
            }
        }

        override fun executeIntent(intent: ListingEditorIntent) {
            dispatch(intent)

            if (intent is ListingEditorIntent.Submit) {
                handleSubmit()
            }
        }

        private fun handleSubmit() {
            val currentState = state()

            if (!currentState.isSubmitting || currentState.error != null) {
                return
            }

            scope.launch {
                val userProfile = sessionManager.getCurrentUserProfile()
                if (userProfile == null) {
                    dispatch(ListingEditorIntent.SetError("User not authenticated"))
                    return@launch
                }

                if (listingId == null) {
                    createListing(currentState, userProfile.id, userProfile.email)
                } else {
                    updateListing(listingId, currentState)
                }
            }
        }

        private suspend fun createListing(
            state: ListingEditorState,
            userId: UserId,
            email: Email
        ) {
            val newListing = state.toNewListing(userId, email)

            when (val result = listingRepository.createListing(newListing)) {
                is RepositoryOperationResult.Success -> {
                    when (result.value) {
                        is InsertResult.Inserted -> {
                            publish(ListingEditorLabel.ListingCreated)
                            dispatch(ListingEditorIntent.SetSubmitting(false))
                        }

                        is InsertResult.AlreadyExists -> {
                            dispatch(ListingEditorIntent.SetError("Listing already exists"))
                        }
                    }
                }

                is RepositoryOperationResult.Failure -> {
                    publish(ListingEditorLabel.ErrorOccurred(result.error))
                    dispatch(ListingEditorIntent.SetSubmitting(false))
                }
            }
        }

        private suspend fun updateListing(id: ListingId, state: ListingEditorState) {
            val patch = state.toPatchRequest()

            when (val result = listingRepository.updateListing(id, patch)) {
                is RepositoryOperationResult.Success -> {
                    when (result.value) {
                        is UpdateResult.Updated -> {
                            publish(ListingEditorLabel.ListingUpdated)
                            dispatch(ListingEditorIntent.SetSubmitting(false))
                        }

                        is UpdateResult.NotFound -> {
                            dispatch(ListingEditorIntent.SetError("Listing not found"))
                        }

                        is UpdateResult.Forbidden -> {
                            dispatch(ListingEditorIntent.SetError("Operation forbidden"))
                        }
                    }
                }

                is RepositoryOperationResult.Failure -> {
                    publish(ListingEditorLabel.ErrorOccurred(result.error))
                    dispatch(ListingEditorIntent.SetSubmitting(false))
                }
            }
        }
    }

    private object ListingEditorReducer : Reducer<ListingEditorState, ListingEditorIntent> {
        override fun ListingEditorState.reduce(msg: ListingEditorIntent): ListingEditorState {
            return reduceListingEditor(this, msg)
        }
    }
}