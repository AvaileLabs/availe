package com.availelabs.availe

import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.*
import com.availelabs.availe.listingeditor.ListingEditorPageConfig
import com.availelabs.availe.model.ListingDraft
import io.availe.models.Patchable
import kotlinx.datetime.*

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
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val timeZoneIdentifier: TimeZoneIdentifier = TimeZoneIdentifier(TimeZone.currentSystemDefault().id),
    val timeResolutionError: LocalTimeResolution? = null,
    val type: ListingCategory? = null,
    val isSubmitting: Boolean = false,
    val error: ListingEditorError? = null,
    val showDiscardDraftDialog: Boolean = false,
    val showDeleteDraftConfirmation: Boolean = false,
    val showPublishConfirmationDialog: Boolean = false,
    val isBasicsStepValid: Boolean = false,
    val isPricingStepValid: Boolean = false,
    val isScheduleStepValid: Boolean = false
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
    data class ChangeStartDate(val startDate: LocalDate?) : ListingEditorIntent
    data class ChangeStartTime(val startTime: LocalTime?) : ListingEditorIntent
    data class ChangeEndDate(val endDate: LocalDate?) : ListingEditorIntent
    data class ChangeEndTime(val endTime: LocalTime?) : ListingEditorIntent
    data class ChangeTimeZoneIdentifier(val timeZoneIdentifier: TimeZoneIdentifier) : ListingEditorIntent
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
                startDate = intent.draft.startDate,
                startTime = intent.draft.startTime,
                endDate = intent.draft.endDate,
                endTime = intent.draft.endTime,
                timeZoneIdentifier = intent.draft.timeZoneIdentifier,
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

        is ListingEditorIntent.ChangeStartDate -> {
            state.copy(startDate = intent.startDate, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeStartTime -> {
            state.copy(startTime = intent.startTime, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeEndDate -> {
            state.copy(endDate = intent.endDate, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeEndTime -> {
            state.copy(endTime = intent.endTime, error = null).recalculateValidation()
        }

        is ListingEditorIntent.ChangeTimeZoneIdentifier -> {
            state.copy(timeZoneIdentifier = intent.timeZoneIdentifier, error = null).recalculateValidation()
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
            startDate = null,
            startTime = null,
            endDate = null,
            endTime = null,
            timeZoneIdentifier = TimeZoneIdentifier(TimeZone.currentSystemDefault().id),
            timeResolutionError = null,
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

                ListingEditorPageConfig.Schedule -> state.copy(
                    startDate = null,
                    startTime = null,
                    endDate = null,
                    endTime = null
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

    var scheduleValid = true
    var calculatedTimeResolutionError: LocalTimeResolution? = null

    if (this.type == ListingCategory.Event) {
        if (this.startDate != null && this.startTime != null && this.endDate != null && this.endTime != null) {
            try {
                val timeZone = TimeZone.of(this.timeZoneIdentifier.value)

                val startLocalDateTime = LocalDateTime(this.startDate, this.startTime)
                val startInstant = startLocalDateTime.toInstant(timeZone)
                val isStartSkipped = startInstant.toLocalDateTime(timeZone) != startLocalDateTime

                val endLocalDateTime = LocalDateTime(this.endDate, this.endTime)
                val endInstant = endLocalDateTime.toInstant(timeZone)
                val isEndSkipped = endInstant.toLocalDateTime(timeZone) != endLocalDateTime

                if (isStartSkipped || isEndSkipped) {
                    scheduleValid = false
                    calculatedTimeResolutionError = LocalTimeResolution.Skipped
                } else {
                    if (endInstant <= startInstant) {
                        scheduleValid = false
                    }
                }
            } catch (exception: Exception) {
                scheduleValid = false
            }
        } else {
            scheduleValid = false
        }
    }

    return this.copy(
        isBasicsStepValid = basicsValid,
        isPricingStepValid = pricingValid,
        isScheduleStepValid = scheduleValid,
        timeResolutionError = calculatedTimeResolutionError
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
        ListingCategory.Event -> {
            if (!this.isScheduleStepValid || this.startDate == null || this.startTime == null || this.endDate == null || this.endTime == null) return null

            val timeZone = TimeZone.of(this.timeZoneIdentifier.value)
            val startLocal = LocalDateTime(this.startDate, this.startTime)
            val endLocal = LocalDateTime(this.endDate, this.endTime)

            val schedule = EventSchedule(
                startLocalTime = startLocal,
                endLocalTime = endLocal,
                startInstant = startLocal.toInstant(timeZone),
                endInstant = endLocal.toInstant(timeZone),
                timeZoneIdentifier = this.timeZoneIdentifier
            )

            NewListing.Event(
                commonData = commonData,
                publisher = publisher,
                schedule = schedule,
                autoRsvp = true
            )
        }

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

        ListingCategory.Event -> {
            val schedulePatch =
                if (this.startDate != null && this.startTime != null && this.endDate != null && this.endTime != null && this.isScheduleStepValid) {
                    val timeZone = TimeZone.of(this.timeZoneIdentifier.value)
                    val startLocal = LocalDateTime(this.startDate, this.startTime)
                    val endLocal = LocalDateTime(this.endDate, this.endTime)
                    Patchable.Set(
                        EventScheduleDtoSchema.V1.PatchRequest(
                            startLocalTime = Patchable.Set(startLocal),
                            endLocalTime = Patchable.Set(endLocal),
                            startInstant = Patchable.Set(startLocal.toInstant(timeZone)),
                            endInstant = Patchable.Set(endLocal.toInstant(timeZone)),
                            timeZoneIdentifier = Patchable.Set(this.timeZoneIdentifier)
                        )
                    )
                } else {
                    Patchable.Unchanged
                }

            ListingEventDtoSchema.V1.PatchRequest(
                title = Patchable.Set(this.title),
                description = Patchable.Set(descriptionValue),
                price = Patchable.Set(priceObj),
                imageLinks = Patchable.Set(imageLinks),
                schedule = schedulePatch
            )
        }

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
        startDate = this.startDate,
        startTime = this.startTime,
        endDate = this.endDate,
        endTime = this.endTime,
        timeZoneIdentifier = this.timeZoneIdentifier,
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
    val schedule = (this as? Listing.Event)?.schedule

    val currency = SupportedCurrency.fromCode(commonData.price.currency) ?: SupportedCurrency.USD

    return ListingEditorState(
        isEditing = true,
        title = commonData.title,
        description = commonData.description ?: ListingDescription(""),
        priceAmount = commonData.price.amount,
        priceCurrency = currency,
        imageUrl = commonData.imageLinks.firstOrNull() ?: ImageUrl(""),
        startDate = schedule?.startLocalTime?.date,
        startTime = schedule?.startLocalTime?.time,
        endDate = schedule?.endLocalTime?.date,
        endTime = schedule?.endLocalTime?.time,
        timeZoneIdentifier = schedule?.timeZoneIdentifier ?: TimeZoneIdentifier(TimeZone.currentSystemDefault().id),
        type = category,
        error = null,
        isSubmitting = false,
        showDiscardDraftDialog = false,
        showPublishConfirmationDialog = false
    ).recalculateValidation()
}