package com.availelabs.availe

import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.listingeditor.ListingEditorPageConfig
import com.availelabs.availe.listingeditor.toEditorState
import com.availelabs.availe.model.ListingDraft
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
    val scheduleBounds: DraftEventBounds = DraftEventBounds(),
    val timeResolutionError: LocalTimeResolution? = null,
    val type: ListingCategory? = null,
    val isSubmitting: Boolean = false,
    val error: ListingEditorError? = null,
    val showDiscardDraftDialog: Boolean = false,
    val showDeleteDraftConfirmation: Boolean = false,
    val showPublishConfirmationDialog: Boolean = false,
    val pendingScheduleAdjustment: PendingScheduleAdjustment? = null,
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
    data object ConfirmScheduleAdjustment : ListingEditorIntent
    data object CancelScheduleAdjustment : ListingEditorIntent
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
            val restoredBounds = DraftEventBounds(
                startDate = intent.draft.startDate,
                startTime = intent.draft.startTime,
                endDate = intent.draft.endDate,
                endTime = intent.draft.endTime,
                timeZoneIdentifier = intent.draft.timeZoneIdentifier
            )
            state.copy(
                title = intent.draft.title,
                description = intent.draft.description,
                priceAmount = intent.draft.priceAmount,
                priceCurrency = currency,
                imageUrl = intent.draft.imageUrl,
                scheduleBounds = restoredBounds,
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
            val currentBounds = state.scheduleBounds
            val newStartDate = intent.startDate
            if (newStartDate != null && currentBounds.endDate != null && newStartDate > currentBounds.endDate) {
                val proposedBounds = currentBounds.copy(startDate = newStartDate, endDate = newStartDate)
                state.copy(pendingScheduleAdjustment = PendingScheduleAdjustment.ShiftEndDate(proposedBounds))
            } else {
                state.copy(scheduleBounds = currentBounds.copy(startDate = newStartDate)).recalculateValidation()
            }
        }

        is ListingEditorIntent.ChangeStartTime -> {
            val currentBounds = state.scheduleBounds
            val newStartTime = intent.startTime
            if (newStartTime != null && currentBounds.startDate != null && currentBounds.endDate != null && currentBounds.startDate == currentBounds.endDate && currentBounds.endTime != null && newStartTime > currentBounds.endTime) {
                val proposedBounds = currentBounds.copy(startTime = newStartTime, endTime = newStartTime)
                state.copy(pendingScheduleAdjustment = PendingScheduleAdjustment.ShiftEndTime(proposedBounds))
            } else {
                state.copy(scheduleBounds = currentBounds.copy(startTime = newStartTime)).recalculateValidation()
            }
        }

        is ListingEditorIntent.ChangeEndDate -> {
            val currentBounds = state.scheduleBounds
            val newEndDate = intent.endDate
            if (newEndDate != null && currentBounds.startDate != null && newEndDate < currentBounds.startDate) {
                val proposedBounds = currentBounds.copy(endDate = newEndDate, startDate = newEndDate)
                state.copy(pendingScheduleAdjustment = PendingScheduleAdjustment.ShiftStartDate(proposedBounds))
            } else {
                state.copy(scheduleBounds = currentBounds.copy(endDate = newEndDate)).recalculateValidation()
            }
        }

        is ListingEditorIntent.ChangeEndTime -> {
            val currentBounds = state.scheduleBounds
            val newEndTime = intent.endTime
            if (newEndTime != null && currentBounds.startDate != null && currentBounds.endDate != null && currentBounds.startDate == currentBounds.endDate && currentBounds.startTime != null && newEndTime < currentBounds.startTime) {
                val proposedBounds = currentBounds.copy(endTime = newEndTime, startTime = newEndTime)
                state.copy(pendingScheduleAdjustment = PendingScheduleAdjustment.ShiftStartTime(proposedBounds))
            } else {
                state.copy(scheduleBounds = currentBounds.copy(endTime = newEndTime)).recalculateValidation()
            }
        }

        is ListingEditorIntent.ChangeTimeZoneIdentifier -> {
            val currentBounds = state.scheduleBounds
            val updatedBounds = currentBounds.copy(timeZoneIdentifier = intent.timeZoneIdentifier)
            state.copy(scheduleBounds = updatedBounds, error = null).recalculateValidation()
        }

        ListingEditorIntent.ConfirmScheduleAdjustment -> {
            val pendingAdjustment = state.pendingScheduleAdjustment
            if (pendingAdjustment != null) {
                state.copy(
                    scheduleBounds = pendingAdjustment.proposedBounds,
                    pendingScheduleAdjustment = null
                ).recalculateValidation()
            } else {
                state
            }
        }

        ListingEditorIntent.CancelScheduleAdjustment -> {
            state.copy(pendingScheduleAdjustment = null)
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
            scheduleBounds = DraftEventBounds(),
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
                    scheduleBounds = state.scheduleBounds.copy(
                        startDate = null,
                        startTime = null,
                        endDate = null,
                        endTime = null
                    )
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
        if (this.scheduleBounds.startDate != null && this.scheduleBounds.startTime != null && this.scheduleBounds.endDate != null && this.scheduleBounds.endTime != null) {
            try {
                val timeZone = TimeZone.of(this.scheduleBounds.timeZoneIdentifier.value)

                val startLocalDateTime = LocalDateTime(this.scheduleBounds.startDate, this.scheduleBounds.startTime)
                val startInstant = startLocalDateTime.toInstant(timeZone)
                val isStartSkipped = startInstant.toLocalDateTime(timeZone) != startLocalDateTime

                val endLocalDateTime = LocalDateTime(this.scheduleBounds.endDate, this.scheduleBounds.endTime)
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