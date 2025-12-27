package com.availelabs.availe

import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import io.availe.models.Patchable

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
    data class LoadData(val listing: Listing) : ListingEditorIntent
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
        is ListingEditorIntent.LoadData -> {
            val commonData = intent.listing.commonData
            val category = when (intent.listing) {
                is Listing.Item -> ListingCategory.Item
                is Listing.Event -> ListingCategory.Event
                is Listing.Service -> ListingCategory.Service
            }

            val currency = SupportedCurrency.fromCode(commonData.price.currency) ?: SupportedCurrency.USD

            state.copy(
                isEditing = true,
                title = commonData.title,
                description = commonData.description ?: ListingDescription(""),
                priceAmount = commonData.price.amount,
                priceCurrency = currency,
                imageUrl = commonData.imageLinks.firstOrNull() ?: ImageUrl(""),
                type = category,
                error = null,
                isSubmitting = false
            )
        }

        is ListingEditorIntent.ChangeTitle -> state.copy(title = intent.title)
        is ListingEditorIntent.ChangeDescription -> state.copy(description = intent.value)
        is ListingEditorIntent.ChangePrice -> {
            if (intent.amount.isValid()) {
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
            if (!state.title.isValid()) {
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

fun ListingEditorState.toNewListing(publisherId: UserId, publisherEmail: Email): NewListing {
    val price = Price(this.priceAmount, this.priceCurrency.code)
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()

    val commonData = CommonListingData(
        title = this.title,
        description = if (this.description.value.isBlank()) null else this.description,
        price = price,
        imageLinks = imageLinks,
        publisherId = publisherId,
        publisherEmail = publisherEmail
    )

    return when (this.type) {
        ListingCategory.Item -> NewListing.Item(commonData)
        ListingCategory.Event -> NewListing.Event(commonData)
        ListingCategory.Service -> NewListing.Service(commonData)
    }
}

fun ListingEditorState.toPatchRequest(): TypeV1ListingDtoPatchRequest {
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()
    val descriptionValue = if (this.description.value.isBlank()) null else this.description

    val priceObj = Price(this.priceAmount, this.priceCurrency.code)

    return when (this.type) {
        ListingCategory.Item -> ListingItemDtoSchema.V1.PatchRequest(
            title = Patchable.Set(this.title),
            description = Patchable.Set(descriptionValue),
            price = Patchable.Set(priceObj),
            imageLinks = Patchable.Set(imageLinks)
        )

        ListingCategory.Event -> ListingEventDtoSchema.V1.PatchRequest(
            title = Patchable.Set(this.title),
            description = Patchable.Set(descriptionValue),
            price = Patchable.Set(priceObj),
            imageLinks = Patchable.Set(imageLinks)
        )

        ListingCategory.Service -> ListingServiceDtoSchema.V1.PatchRequest(
            title = Patchable.Set(this.title),
            description = Patchable.Set(descriptionValue),
            price = Patchable.Set(priceObj),
            imageLinks = Patchable.Set(imageLinks)
        )
    }
}