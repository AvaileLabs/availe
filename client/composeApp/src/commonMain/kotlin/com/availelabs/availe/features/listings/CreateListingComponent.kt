@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.domain.ListingEditorIntent
import com.availelabs.domain.ListingEditorState
import com.availelabs.domain.reduceListingEditor
import com.availelabs.domain.toNewListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface ICreateListingComponent {
    val state: Value<ListingEditorState>
    fun onIntent(intent: ListingEditorIntent)
    fun onCancel()
}

class CreateListingComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val onFinishedCallback: () -> Unit
) : ICreateListingComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)
    private val _state = MutableValue(ListingEditorState())
    override val state: Value<ListingEditorState> = _state

    override fun onIntent(intent: ListingEditorIntent) {
        _state.update { reduceListingEditor(it, intent) }

        if (intent is ListingEditorIntent.Submit) {
            val currentState = _state.value
            if (currentState.isSubmitting && currentState.error == null) {
                performSubmit(currentState)
            }
        }
    }

    private fun performSubmit(currentState: ListingEditorState) {
        scope.launch {
            try {
                val user = authService.getCurrentUserProfile()
                if (user == null) {
                    onIntent(ListingEditorIntent.SetError("User not authenticated"))
                    return@launch
                }

                val newListing = currentState.toNewListing(user.id)
                apiClient.createListing(newListing, user.id)
                onFinishedCallback()
            } catch (e: Exception) {
                onIntent(ListingEditorIntent.SetError(e.message ?: "Unknown error"))
            }
        }
    }

    override fun onCancel() {
        onFinishedCallback()
    }
}