@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.data.network.toDto
import com.availelabs.availe.domain.logic.ListingEditorIntent
import com.availelabs.availe.domain.logic.ListingEditorState
import com.availelabs.availe.domain.logic.reduceListingEditor
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import io.availe.models.SerializablePatchable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface IListingEditorComponent {
    val state: Value<ListingEditorState>
    fun onIntent(intent: ListingEditorIntent)
    fun onCancel()
}

class ListingEditorComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val listingToEdit: Listing? = null,
    private val onFinishedCallback: () -> Unit
) : IListingEditorComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)
    private val _state = MutableValue(ListingEditorState())
    override val state: Value<ListingEditorState> = _state

    init {
        if (listingToEdit != null) {
            val currencyEnum = SupportedCurrency.entries.find { it.code == listingToEdit.price.currency }
                ?: SupportedCurrency.USD

            _state.update {
                it.copy(
                    isEditing = true,
                    title = listingToEdit.title,
                    description = listingToEdit.description ?: "",
                    priceAmount = listingToEdit.price.amount,
                    priceCurrency = currencyEnum,
                    imageUrl = listingToEdit.imageLinks.firstOrNull() ?: "",
                    type = when (listingToEdit) {
                        is Listing.Item -> ListingCategory.Item
                        is Listing.Event -> ListingCategory.Event
                        is Listing.Service -> ListingCategory.Service
                    }
                )
            }
        }
    }

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
                val imageLinks = if (currentState.imageUrl.isNotBlank()) listOf(currentState.imageUrl) else emptyList()

                if (listingToEdit == null) {
                    val commonData = CommonListingData(
                        title = currentState.title,
                        description = currentState.description.ifBlank { null },
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
                } else {
                    val patch: TypeV1ListingDtoPatchRequest = when (listingToEdit) {
                        is Listing.Item -> ListingItemDtoSchema.V1.PatchRequest(
                            title = SerializablePatchable.Set(currentState.title),
                            description = SerializablePatchable.Set(currentState.description.ifBlank { null }),
                            price = SerializablePatchable.Set(price.toDto()),
                            imageLinks = SerializablePatchable.Set(imageLinks)
                        )

                        is Listing.Event -> ListingEventDtoSchema.V1.PatchRequest(
                            title = SerializablePatchable.Set(currentState.title),
                            description = SerializablePatchable.Set(currentState.description.ifBlank { null }),
                            price = SerializablePatchable.Set(price.toDto()),
                            imageLinks = SerializablePatchable.Set(imageLinks)
                        )

                        is Listing.Service -> ListingServiceDtoSchema.V1.PatchRequest(
                            title = SerializablePatchable.Set(currentState.title),
                            description = SerializablePatchable.Set(currentState.description.ifBlank { null }),
                            price = SerializablePatchable.Set(price.toDto()),
                            imageLinks = SerializablePatchable.Set(imageLinks)
                        )
                    }
                    apiClient.updateListing(listingToEdit.id, patch, user.id)
                }
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