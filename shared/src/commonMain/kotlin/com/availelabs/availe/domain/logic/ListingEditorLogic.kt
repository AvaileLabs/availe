package com.availelabs.availe.domain.logic

import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.SupportedCurrency

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

sealed interface ListingEditorIntent {
    data class ChangeTitle(val title: String) : ListingEditorIntent
    data class ChangeDescription(val value: String) : ListingEditorIntent
    data class ChangePrice(val amount: String) : ListingEditorIntent
    data class ChangeCurrency(val currency: SupportedCurrency) : ListingEditorIntent
    data class ChangeType(val type: ListingCategory) : ListingEditorIntent
    data class ChangeImage(val url: String) : ListingEditorIntent
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
            } else if (state.priceAmount.isBlank()) {
                state.copy(error = "Price is required")
            } else {
                state.copy(isSubmitting = true, error = null)
            }
        }

        is ListingEditorIntent.SetSubmitting -> state.copy(isSubmitting = intent.isSubmitting)
        is ListingEditorIntent.SetError -> state.copy(error = intent.message, isSubmitting = false)
    }
}