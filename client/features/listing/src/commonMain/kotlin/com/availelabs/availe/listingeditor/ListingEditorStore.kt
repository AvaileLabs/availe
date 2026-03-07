package com.availelabs.availe.listingeditor

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.*
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.firstOrNull
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
    private val listingId: ListingId?,
    private val initialCategory: ListingCategory? = null
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
        data class InitializeCategory(val category: ListingCategory) : Action
    }

    private inner class ListingEditorBootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            if (listingId != null) {
                scope.launch {
                    var listing = listingRepository.getListingByIdStream(listingId).firstOrNull()

                    if (listing == null) {
                        val fetchResult = listingRepository.fetchAndCacheListingById(listingId)
                        if (fetchResult is RepositoryOperationResult.Success) {
                            listing = listingRepository.getListingByIdStream(listingId).firstOrNull()
                        }
                    }

                    if (listing != null) {
                        dispatch(Action.PopulateListing(listing))
                    }
                }
            } else {
                scope.launch {
                    val draft = listingRepository.getDraft()
                    if (draft != null) {
                        dispatch(Action.RestoreDraft)
                    } else if (initialCategory != null) {
                        dispatch(Action.InitializeCategory(initialCategory))
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

                Action.RestoreDraft -> {
                    scope.launch {
                        val draft = listingRepository.getDraft()
                        if (draft != null) {
                            dispatch(ListingEditorIntent.RestoreDraft(draft))
                            publish(ListingEditorLabel.DraftRestored)
                        }
                    }
                }

                is Action.InitializeCategory -> {
                    dispatch(ListingEditorIntent.ChangeType(action.category))
                }
            }
        }

        override fun executeIntent(intent: ListingEditorIntent) {
            when (intent) {
                is ListingEditorIntent.ConfirmPublish -> submitListing()
                is ListingEditorIntent.ConfirmDiscardDraft -> discardDraft()
                else -> dispatch(intent)
            }
        }

        private fun discardDraft() {
            scope.launch {
                listingRepository.clearDraft()
                dispatch(ListingEditorIntent.ConfirmDiscardDraft)
                publish(ListingEditorLabel.ListingCleared)
            }
        }

        private fun submitListing() {
            dispatch(ListingEditorIntent.ConfirmPublish)

            val currentState = state()

            if (currentState.error != null) {
                return
            }

            scope.launch {
                val userProfile = sessionManager.getCurrentUserProfile()
                if (userProfile == null) {
                    dispatch(ListingEditorIntent.SetError(ListingEditorError.AuthenticationRequired))
                    return@launch
                }

                if (listingId == null) {
                    createListing(
                        state = currentState,
                        publisher = PublicProfile(
                            id = userProfile.id,
                            displayName = UserDisplayName(
                                userProfile.email.value.substringBefore("@").ifBlank { "User" }
                            ),
                            avatarUrl = null,
                            jid = userProfile.jid
                        )
                    )
                } else {
                    updateListing(listingId, currentState)
                }
            }
        }

        private suspend fun createListing(
            state: ListingEditorState,
            publisher: PublicProfile
        ) {
            state.toNewListing(publisher)?.let { newListing ->
                when (val result = listingRepository.createListing(newListing)) {
                    is RepositoryOperationResult.Success -> {
                        when (result.value) {
                            is InsertResult.Inserted -> {
                                listingRepository.clearDraft()
                                publish(ListingEditorLabel.ListingCreated)
                                dispatch(ListingEditorIntent.SetSubmitting(false))
                            }

                            is InsertResult.AlreadyExists -> {
                                dispatch(ListingEditorIntent.SetError(ListingEditorError.ListingExists))
                            }
                        }
                    }

                    is RepositoryOperationResult.Failure -> {
                        publish(ListingEditorLabel.ErrorOccurred(result.error))
                        dispatch(ListingEditorIntent.SetSubmitting(false))
                    }
                }
            } ?: run {
                dispatch(ListingEditorIntent.SetError(ListingEditorError.Generic("Invalid listing state")))
            }
        }

        private suspend fun updateListing(id: ListingId, state: ListingEditorState) {
            state.toPatchRequest()?.let { patch ->
                when (val result = listingRepository.updateListing(id, patch)) {
                    is RepositoryOperationResult.Success -> {
                        when (result.value) {
                            is UpdateResult.Updated -> {
                                publish(ListingEditorLabel.ListingUpdated)
                                dispatch(ListingEditorIntent.SetSubmitting(false))
                            }

                            is UpdateResult.NotFound -> {
                                dispatch(ListingEditorIntent.SetError(ListingEditorError.NotFound))
                            }

                            is UpdateResult.Forbidden -> {
                                dispatch(ListingEditorIntent.SetError(ListingEditorError.Forbidden))
                            }
                        }
                    }

                    is RepositoryOperationResult.Failure -> {
                        publish(ListingEditorLabel.ErrorOccurred(result.error))
                        dispatch(ListingEditorIntent.SetSubmitting(false))
                    }
                }
            } ?: run {
                dispatch(ListingEditorIntent.SetError(ListingEditorError.Generic("Invalid listing state")))
            }
        }
    }

    private object ListingEditorReducer : Reducer<ListingEditorState, ListingEditorIntent> {
        override fun ListingEditorState.reduce(msg: ListingEditorIntent): ListingEditorState {
            return reduceListingEditor(this, msg)
        }
    }
}