@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.domain.logic.ListingEditorIntent
import com.availelabs.availe.domain.logic.ListingEditorState
import com.availelabs.availe.domain.logic.reduceListingEditor
import com.availelabs.availe.domain.model.CommonListingData
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.Price
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

                val price = Price(currentState.priceAmount, currentState.priceCurrency.code)
                val imageLinks =
                    if (currentState.imageUrl.value.isNotBlank()) listOf(currentState.imageUrl) else emptyList()

                val commonData = CommonListingData(
                    title = currentState.title,
                    description = if (currentState.description.value.isBlank()) null else currentState.description,
                    price = price,
                    imageLinks = imageLinks,
                    publisherId = user.id
                )

                val newListing = when (currentState.type) {
                    ListingCategory.Item -> NewListing.Item(commonData)
                    ListingCategory.Event -> NewListing.Event(commonData)
                    ListingCategory.Service -> NewListing.Service(commonData)
                }

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