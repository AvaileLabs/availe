package com.availelabs.availe

import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.listingeditor.ListingEditorPageConfig
import com.availelabs.availe.model.ListingDraft
import io.availe.models.Patchable

sealed interface ListingEditorError {
    data object TitleInvalid : ListingEditorError
    data object PriceRequired : ListingEditorError
    data object AuthenticationRequired : ListingEditorError
    data object ListingExists : ListingEditorError
    data object NotFound : ListingEditorError
    data object Forbidden : ListingEditorError
    data class Generic(val message: String) : ListingEditorError
}

data class ListingEditorState(
    val isEditing: Boolean = false,
    val title: ListingTitle = ListingTitle(""),
    val description: ListingDescription = ListingDescription(""),
    val priceAmount: PriceAmount = PriceAmount(""),
    val priceCurrency: SupportedCurrency = SupportedCurrency.USD,
    val imageUrl: ImageUrl = ImageUrl(""),
    val type: ListingCategory? = null,
    val isSubmitting: Boolean = false,
    val error: ListingEditorError? = null,
    val showDiscardDraftDialog: Boolean = false,
    val showDeleteDraftConfirmation: Boolean = false,
    val showPublishConfirmationDialog: Boolean = false,
    val isBasicsStepValid: Boolean = false,
    val isPricingStepValid: Boolean = false
)

sealed interface ListingEditorIntent {
    data class LoadData(val listing: Listing) : ListingEditorIntent
    data class RestoreDraft(val draft: ListingDraft) : ListingEditorIntent
    data class ChangeTitle(val title: ListingTitle) : ListingEditorIntent
    data class ChangeDescription(val value: ListingDescription) : ListingEditorIntent
    data class ChangePrice(val amount: PriceAmount) : ListingEditorIntent
    data class ChangeCurrency(val currency: SupportedCurrency) : ListingEditorIntent
    data class ChangeType(val type: ListingCategory) : ListingEditorIntent
    data class ChangeImage(val url: ImageUrl) : ListingEditorIntent
    data object AskToPublish : ListingEditorIntent
    data object CancelPublish : ListingEditorIntent
    data object ConfirmPublish : ListingEditorIntent
    data class SetSubmitting(val isSubmitting: Boolean) : ListingEditorIntent
    data class SetError(val error: ListingEditorError?) : ListingEditorIntent
    data object AskToDiscardDraft : ListingEditorIntent
    data object CancelDiscardDraft : ListingEditorIntent
    data object RequestDeleteDraft : ListingEditorIntent
    data object CancelDeleteDraft : ListingEditorIntent
    data object ConfirmDiscardDraft : ListingEditorIntent
    data class ClearCurrentStep(val config: ListingEditorPageConfig) : ListingEditorIntent
}

fun reduceListingEditor(state: ListingEditorState, intent: ListingEditorIntent): ListingEditorState {
    return when (intent) {
        is ListingEditorIntent.LoadData -> {
            intent.listing.toEditorState().recalculateValidation()
        }

        is ListingEditorIntent.RestoreDraft -> {
            val currency = SupportedCurrency.fromCode(intent.draft.priceCurrencyCode) ?: SupportedCurrency.USD
            state.copy(
                title = intent.draft.title,
                description = intent.draft.description,
                priceAmount = intent.draft.priceAmount,
                priceCurrency = currency,
                imageUrl = intent.draft.imageUrl,
                type = intent.draft.type,
                error = null
            ).recalculateValidation()
        }

        is ListingEditorIntent.ChangeTitle -> {
            state.copy(title = intent.title, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeDescription -> {
            state.copy(description = intent.value, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangePrice -> {
            if (intent.amount.isValid()) {
                state.copy(priceAmount = intent.amount, error = null).recalculateValidation()
            } else {
                state
            }
        }

        is ListingEditorIntent.ChangeCurrency -> {
            state.copy(priceCurrency = intent.currency, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeType -> {
            if (!state.isEditing) {
                state.copy(type = intent.type, error = null).recalculateValidation()
            } else {
                state
            }
        }

        is ListingEditorIntent.ChangeImage -> {
            state.copy(imageUrl = intent.url, error = null).recalculateValidation()
        }

        ListingEditorIntent.AskToPublish -> {
            if (!state.isBasicsStepValid) {
                state.copy(error = ListingEditorError.TitleInvalid)
            } else if (!state.isPricingStepValid) {
                state.copy(error = ListingEditorError.PriceRequired)
            } else {
                state.copy(showPublishConfirmationDialog = true, error = null)
            }
        }

        ListingEditorIntent.CancelPublish -> state.copy(showPublishConfirmationDialog = false)
        ListingEditorIntent.ConfirmPublish -> state.copy(showPublishConfirmationDialog = false, isSubmitting = true)

        is ListingEditorIntent.SetSubmitting -> state.copy(isSubmitting = intent.isSubmitting)
        is ListingEditorIntent.SetError -> state.copy(error = intent.error, isSubmitting = false)

        ListingEditorIntent.AskToDiscardDraft -> state.copy(showDiscardDraftDialog = true)
        ListingEditorIntent.CancelDiscardDraft -> state.copy(showDiscardDraftDialog = false)

        ListingEditorIntent.RequestDeleteDraft -> state.copy(
            showDiscardDraftDialog = false,
            showDeleteDraftConfirmation = true
        )

        ListingEditorIntent.CancelDeleteDraft -> state.copy(showDeleteDraftConfirmation = false)

        ListingEditorIntent.ConfirmDiscardDraft -> state.copy(
            showDiscardDraftDialog = false,
            showDeleteDraftConfirmation = false,
            title = ListingTitle(""),
            description = ListingDescription(""),
            priceAmount = PriceAmount(""),
            priceCurrency = SupportedCurrency.USD,
            imageUrl = ImageUrl(""),
            type = if (state.isEditing) state.type else null,
            error = null
        ).recalculateValidation()

        is ListingEditorIntent.ClearCurrentStep -> {
            val newState = when (intent.config) {
                ListingEditorPageConfig.Type -> {
                    if (!state.isEditing) {
                        state.copy(type = null)
                    } else {
                        state
                    }
                }

                ListingEditorPageConfig.Basics -> state.copy(
                    title = ListingTitle(""),
                    description = ListingDescription("")
                )

                ListingEditorPageConfig.Pricing -> state.copy(
                    priceAmount = PriceAmount("")
                )

                ListingEditorPageConfig.Media -> state.copy(
                    imageUrl = ImageUrl("")
                )

                ListingEditorPageConfig.Review -> state
            }
            newState.copy(showDiscardDraftDialog = false, error = null).recalculateValidation()
        }
    }
}

private fun ListingEditorState.recalculateValidation(): ListingEditorState {
    val basicsValid = this.title.isValid()
    val pricingValid = this.priceAmount.isValid() && this.priceAmount.value.isNotBlank()

    return this.copy(
        isBasicsStepValid = basicsValid,
        isPricingStepValid = pricingValid
    )
}

fun ListingEditorState.toNewListing(publisher: PublicProfile): NewListing? {
    val type = this.type ?: return null
    val price = Price(this.priceAmount, this.priceCurrency.code)
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()

    val commonData = CommonListingData(
        title = this.title,
        description = if (this.description.value.isBlank()) null else this.description,
        price = price,
        imageLinks = imageLinks,
        location = ListingLocation("Unknown")
    )

    return when (type) {
        ListingCategory.Item -> NewListing.Item(commonData, publisher)
        ListingCategory.Event -> NewListing.Event(commonData, publisher)
        ListingCategory.Service -> NewListing.Service(commonData, publisher)
    }
}

fun ListingEditorState.toPatchRequest(): TypeV1ListingDtoPatchRequest? {
    val type = this.type ?: return null
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()
    val descriptionValue = if (this.description.value.isBlank()) null else this.description

    val priceObj = Price(this.priceAmount, this.priceCurrency.code)

    return when (type) {
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

fun ListingEditorState.toDraft(stepIndex: Int): ListingDraft? {
    val type = this.type ?: return null
    return ListingDraft(
        title = this.title,
        description = this.description,
        priceAmount = this.priceAmount,
        priceCurrencyCode = this.priceCurrency.code,
        imageUrl = this.imageUrl,
        type = type,
        stepIndex = stepIndex
    )
}

private fun Listing.toEditorState(): ListingEditorState {
    val commonData = this.commonData
    val category = when (this) {
        is Listing.Item -> ListingCategory.Item
        is Listing.Event -> ListingCategory.Event
        is Listing.Service -> ListingCategory.Service
    }

    val currency = SupportedCurrency.fromCode(commonData.price.currency) ?: SupportedCurrency.USD

    return ListingEditorState(
        isEditing = true,
        title = commonData.title,
        description = commonData.description ?: ListingDescription(""),
        priceAmount = commonData.price.amount,
        priceCurrency = currency,
        imageUrl = commonData.imageLinks.firstOrNull() ?: ImageUrl(""),
        type = category,
        error = null,
        isSubmitting = false,
        showDiscardDraftDialog = false,
        showPublishConfirmationDialog = false
    )
}