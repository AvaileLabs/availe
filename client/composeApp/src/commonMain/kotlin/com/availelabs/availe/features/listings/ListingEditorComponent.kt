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
import com.availelabs.availe.domain.model.CommonListingData
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.Price
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

    fun onTitleChanged(title: String)
    fun onDescriptionChanged(description: String)
    fun onPriceAmountChanged(amount: String)
    fun onCurrencyChanged(currency: String)
    fun onImageUrlChanged(url: String)
    fun onTypeChanged(type: ListingType)
    fun onSubmit()
    fun onCancel()
}

data class ListingEditorState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priceAmount: String = "",
    val priceCurrency: String = "USD",
    val imageUrl: String = "",
    val type: ListingType = ListingType.Item,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

enum class ListingType {
    Item, Event, Service
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
            _state.update {
                it.copy(
                    isEditing = true,
                    title = listingToEdit.title,
                    description = listingToEdit.description ?: "",
                    priceAmount = listingToEdit.price.amount,
                    priceCurrency = listingToEdit.price.currency,
                    imageUrl = listingToEdit.imageLinks.firstOrNull() ?: "",
                    type = when (listingToEdit) {
                        is Listing.Item -> ListingType.Item
                        is Listing.Event -> ListingType.Event
                        is Listing.Service -> ListingType.Service
                    }
                )
            }
        }
    }

    override fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title) }
    }

    override fun onDescriptionChanged(description: String) {
        _state.update { it.copy(description = description) }
    }

    override fun onPriceAmountChanged(amount: String) {
        if (amount.all { it.isDigit() || it == '.' }) {
            _state.update { it.copy(priceAmount = amount) }
        }
    }

    override fun onCurrencyChanged(currency: String) {
        _state.update { it.copy(priceCurrency = currency) }
    }

    override fun onImageUrlChanged(url: String) {
        _state.update { it.copy(imageUrl = url) }
    }

    override fun onTypeChanged(type: ListingType) {
        if (listingToEdit == null) {
            _state.update { it.copy(type = type) }
        }
    }

    override fun onSubmit() {
        val currentState = _state.value
        if (currentState.title.isBlank() || currentState.priceAmount.isBlank()) {
            _state.update { it.copy(error = "Title and Price are required") }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }

        scope.launch {
            try {
                val user = authService.getCurrentUserProfile()
                if (user == null) {
                    _state.update { it.copy(isSubmitting = false, error = "User not authenticated") }
                    return@launch
                }

                val price = Price(currentState.priceAmount, currentState.priceCurrency)
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
                        ListingType.Item -> NewListing.Item(commonData)
                        ListingType.Event -> NewListing.Event(commonData)
                        ListingType.Service -> NewListing.Service(commonData)
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
                _state.update { it.copy(isSubmitting = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    override fun onCancel() {
        onFinishedCallback()
    }
}