package com.availelabs.domain

import com.availelabs.availe.domain.model.*

data class ListingEditorState(
    val isEditing: Boolean = false,
    val title: ListingTitle = ListingTitle(""),
    val description: ListingDescription = ListingDescription(""),
    val priceAmount: PriceAmount = PriceAmount(""),
    val priceCurrency: SupportedCurrency = SupportedCurrency.USD,
    val imageUrl: ImageUrl = ImageUrl(""),
    val type: ListingCategory = ListingCategory.Item,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

sealed interface ListingEditorIntent {
    data class ChangeTitle(val title: ListingTitle) : ListingEditorIntent
    data class ChangeDescription(val value: ListingDescription) : ListingEditorIntent
    data class ChangePrice(val amount: PriceAmount) : ListingEditorIntent
    data class ChangeCurrency(val currency: SupportedCurrency) : ListingEditorIntent
    data class ChangeType(val type: ListingCategory) : ListingEditorIntent
    data class ChangeImage(val url: ImageUrl) : ListingEditorIntent
    data object Submit : ListingEditorIntent
    data class SetSubmitting(val isSubmitting: Boolean) : ListingEditorIntent
    data class SetError(val message: String?) : ListingEditorIntent
}

fun reduceListingEditor(state: ListingEditorState, intent: ListingEditorIntent): ListingEditorState {
    return when (intent) {
        is ListingEditorIntent.ChangeTitle -> state.copy(title = intent.title)
        is ListingEditorIntent.ChangeDescription -> state.copy(description = intent.value)
        is ListingEditorIntent.ChangePrice -> {
            if (InputValidator.isValidPrice(intent.amount)) {
                state.copy(priceAmount = intent.amount)
            } else {
                state
            }
        }

        is ListingEditorIntent.ChangeCurrency -> state.copy(priceCurrency = intent.currency)
        is ListingEditorIntent.ChangeType -> {
            if (!state.isEditing) {
                state.copy(type = intent.type)
            } else {
                state
            }
        }

        is ListingEditorIntent.ChangeImage -> state.copy(imageUrl = intent.url)
        is ListingEditorIntent.Submit -> {
            if (!InputValidator.isValidTitle(state.title)) {
                state.copy(error = "Title is invalid")
            } else if (state.priceAmount.value.isBlank()) {
                state.copy(error = "Price is required")
            } else {
                state.copy(isSubmitting = true, error = null)
            }
        }

        is ListingEditorIntent.SetSubmitting -> state.copy(isSubmitting = intent.isSubmitting)
        is ListingEditorIntent.SetError -> state.copy(error = intent.message, isSubmitting = false)
    }
}

fun ListingEditorState.toNewListing(publisherId: UserId): NewListing {
    val price = Price(this.priceAmount, this.priceCurrency.code)
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()

    val commonData = CommonListingData(
        title = this.title,
        description = if (this.description.value.isBlank()) null else this.description,
        price = price,
        imageLinks = imageLinks,
        publisherId = publisherId
    )

    return when (this.type) {
        ListingCategory.Item -> NewListing.Item(commonData)
        ListingCategory.Event -> NewListing.Event(commonData)
        ListingCategory.Service -> NewListing.Service(commonData)
    }
}