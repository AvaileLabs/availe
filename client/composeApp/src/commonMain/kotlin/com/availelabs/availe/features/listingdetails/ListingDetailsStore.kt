@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.availelabs.availe.features.listingdetails

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.data.repositories.IListingRepository
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.RepositoryOperationResult
import com.availelabs.availe.features.listingdetails.ListingDetailsStore.*
import com.availelabs.availe.ui.mapper.ErrorMessageMapper
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.launch

interface ListingDetailsStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object RetryRequested : Intent
    }

    data class State(
        val listing: Listing? = null,
        val isLoading: Boolean = false,
        val errorMessage: UiText? = null
    )

    sealed interface Label
}

class ListingDetailsStoreFactory(
    private val storeFactory: StoreFactory,
    private val listingRepository: IListingRepository,
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
    }

    private sealed interface Message {
        data class SetLoading(val isLoading: Boolean) : Message
        data class ListingLoaded(val listing: Listing) : Message
        data class SetError(val message: UiText?) : Message
    }

    private class ListingDetailsBootstrapper(
        private val listingId: ListingId
    ) : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.FetchListing(listingId))
        }
    }

    private inner class ListingDetailsExecutor : CoroutineExecutor<Intent, Action, State, Message, Label>() {
        override fun executeAction(action: Action) {
            when (action) {
                is Action.FetchListing -> fetchListing(action.listingId)
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.RetryRequested -> fetchListing(listingId)
            }
        }

        private fun fetchListing(id: ListingId) {
            dispatch(Message.SetLoading(true))
            dispatch(Message.SetError(null))

            scope.launch {
                when (val result = listingRepository.getListingById(id)) {
                    is RepositoryOperationResult.Success<*> -> {
                        when (val findResult = result.value) {
                            is FindResult.Found<*> -> {
                                dispatch(Message.ListingLoaded(findResult.value as Listing))
                            }

                            is FindResult.NotFound -> {
                                dispatch(Message.SetError(UiText.VerbatimString("Listing not found")))
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
    }

    private object ListingDetailsReducer : Reducer<State, Message> {
        override fun State.reduce(msg: Message): State =
            when (msg) {
                is Message.SetLoading -> copy(isLoading = msg.isLoading)
                is Message.ListingLoaded -> copy(listing = msg.listing)
                is Message.SetError -> copy(errorMessage = msg.message)
            }
    }
}