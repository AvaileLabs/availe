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

    fun onTitleChanged(title: String)
    fun onDescriptionChanged(description: String)
    fun onPriceAmountChanged(amount: String)
    fun onCurrencyChanged(currency: SupportedCurrency)
    fun onImageUrlChanged(url: String)
    fun onTypeChanged(type: ListingCategory)
    fun onSubmit()
    fun onCancel()
}

data class ListingEditorState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priceAmount: String = "",
    val priceCurrency: SupportedCurrency = SupportedCurrency.USD,
    val imageUrl: String = "",
    val type: ListingCategory = ListingCategory.Item,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

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

    override fun onCurrencyChanged(currency: SupportedCurrency) {
        _state.update { it.copy(priceCurrency = currency) }
    }

    override fun onImageUrlChanged(url: String) {
        _state.update { it.copy(imageUrl = url) }
    }

    override fun onTypeChanged(type: ListingCategory) {
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
                _state.update { it.copy(isSubmitting = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    override fun onCancel() {
        onFinishedCallback()
    }
}