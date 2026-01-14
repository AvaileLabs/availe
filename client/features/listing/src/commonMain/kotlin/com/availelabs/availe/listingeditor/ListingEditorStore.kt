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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface IListingEditorStore : Store<ListingEditorIntent, ListingEditorState, ListingEditorLabel>

sealed interface ListingEditorLabel {
    data object ListingCreated : ListingEditorLabel
    data object ListingUpdated : ListingEditorLabel
    data object ListingCleared : ListingEditorLabel
    data class ErrorOccurred(val error: AppError) : ListingEditorLabel
    data object DraftRestored : ListingEditorLabel
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
        data object RestoreDraft : Action
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
            } else {
                scope.launch {
                    val draft = listingRepository.getDraft()
                    if (draft != null) {
                        dispatch(Action.RestoreDraft)
                    }
                }
            }
        }
    }

    private inner class ListingEditorExecutor :
        CoroutineExecutor<ListingEditorIntent, Action, ListingEditorState, ListingEditorIntent, ListingEditorLabel>() {

        private var autoSaveJob: Job? = null

        override fun executeAction(action: Action) {
            when (action) {
                is Action.PopulateListing -> {
                    dispatch(ListingEditorIntent.LoadData(action.listing))
                }

                Action.RestoreDraft -> {
                    scope.launch {
                        val draft = listingRepository.getDraft()
                        if (draft != null) {
                            dispatch(ListingEditorIntent.RestoreDraft(draft))
                            publish(ListingEditorLabel.DraftRestored)
                        }
                    }
                }
            }
        }

        override fun executeIntent(intent: ListingEditorIntent) {
            dispatch(intent)

            when (intent) {
                is ListingEditorIntent.Submit -> handleSubmit()
                ListingEditorIntent.ConfirmClear -> handleClear()
                is ListingEditorIntent.ChangeTitle,
                is ListingEditorIntent.ChangeDescription,
                is ListingEditorIntent.ChangePrice,
                is ListingEditorIntent.ChangeCurrency,
                is ListingEditorIntent.ChangeImage,
                is ListingEditorIntent.ChangeType -> triggerAutoSave()

                else -> {}
            }
        }

        private fun triggerAutoSave() {
            if (listingId != null) return // Don't auto-save drafts when editing existing listings, for now.

            autoSaveJob?.cancel()
            autoSaveJob = scope.launch {
                delay(500) // Debounce
                val currentState = state()
                // Only save if it has some meaningful data
                if (currentState.title.value.isNotEmpty() || currentState.priceAmount.value.isNotEmpty()) {
                    listingRepository.saveDraft(currentState.toDraft())
                }
            }
        }

        private fun handleClear() {
            scope.launch {
                listingRepository.clearDraft()
                publish(ListingEditorLabel.ListingCleared)
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
                            listingRepository.clearDraft() // Clear draft on success
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