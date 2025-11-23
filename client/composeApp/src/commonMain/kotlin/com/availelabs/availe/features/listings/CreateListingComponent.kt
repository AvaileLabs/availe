@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface ICreateListingComponent {
    val state: Value<CreateListingState>

    fun onTitleChanged(title: String)
    fun onDescriptionChanged(description: String)
    fun onPriceAmountChanged(amount: String)
    fun onCurrencyChanged(currency: SupportedCurrency)
    fun onImageUrlChanged(url: String)
    fun onTypeChanged(type: ListingCategory)
    fun onSubmit()
    fun onCancel()
}

data class CreateListingState(
    val title: String = "",
    val description: String = "",
    val priceAmount: String = "",
    val priceCurrency: SupportedCurrency = SupportedCurrency.USD,
    val imageUrl: String = "",
    val type: ListingCategory = ListingCategory.Item,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class CreateListingComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val onFinishedCallback: () -> Unit
) : ICreateListingComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)
    private val _state = MutableValue(CreateListingState())
    override val state: Value<CreateListingState> = _state

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
        _state.update { it.copy(type = type) }
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