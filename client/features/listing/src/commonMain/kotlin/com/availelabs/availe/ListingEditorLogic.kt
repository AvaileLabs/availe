package com.availelabs.availe

import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.listingeditor.ListingEditorPageConfig
import com.availelabs.availe.listingeditor.toEditorState
import com.availelabs.availe.listingeditor.toLocalDateOrNull
import com.availelabs.availe.listingeditor.toLocalTimeOrNull
import com.availelabs.availe.model.ListingDraft
import com.availelabs.availe.ui.model.MeridiemIndicator
import com.availelabs.availe.ui.model.SegmentedDateState
import com.availelabs.availe.ui.model.SegmentedTimeState
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
    val startDate: SegmentedDateState = SegmentedDateState(),
    val startTime: SegmentedTimeState = SegmentedTimeState(),
    val endDate: SegmentedDateState = SegmentedDateState(),
    val endTime: SegmentedTimeState = SegmentedTimeState(),
    val timeZoneIdentifier: TimeZoneIdentifier = TimeZoneIdentifier(TimeZone.currentSystemDefault().id),
    val isTwentyFourHourTimeFormat: Boolean = false,
    val type: ListingCategory? = null,
    val isSubmitting: Boolean = false,
    val error: ListingEditorError? = null,
    val showDiscardDraftDialog: Boolean = false,
    val showDeleteDraftConfirmation: Boolean = false,
    val showPublishConfirmationDialog: Boolean = false,
    val isBasicsStepValid: Boolean = false,
    val isPricingStepValid: Boolean = false,
    val scheduleValidationState: ScheduleValidationState = ScheduleValidationState.Incomplete
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

    data class UpdateStartDateMonth(val monthString: String) : ListingEditorIntent
    data class UpdateStartDateDay(val dayString: String) : ListingEditorIntent
    data class UpdateStartDateYear(val yearString: String) : ListingEditorIntent

    data class UpdateStartTimeHour(val hourString: String) : ListingEditorIntent
    data class UpdateStartTimeMinute(val minuteString: String) : ListingEditorIntent
    data object ToggleStartTimeMeridiem : ListingEditorIntent

    data class UpdateEndDateMonth(val monthString: String) : ListingEditorIntent
    data class UpdateEndDateDay(val dayString: String) : ListingEditorIntent
    data class UpdateEndDateYear(val yearString: String) : ListingEditorIntent

    data class UpdateEndTimeHour(val hourString: String) : ListingEditorIntent
    data class UpdateEndTimeMinute(val minuteString: String) : ListingEditorIntent
    data object ToggleEndTimeMeridiem : ListingEditorIntent

    data class ChangeTimeZoneIdentifier(val timeZoneIdentifier: TimeZoneIdentifier) : ListingEditorIntent
    data class SetTwentyFourHourTimeFormat(val isTwentyFourHour: Boolean) : ListingEditorIntent

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

internal fun String.filterDigitsAndTruncate(maximumLength: Int): String {
    return this.filter { character -> character.isDigit() }.take(maximumLength)
}

internal fun LocalDate.toSegmentedDateState(): SegmentedDateState {
    return SegmentedDateState(
        monthInputString = this.monthNumber.toString().padStart(2, '0'),
        dayInputString = this.dayOfMonth.toString().padStart(2, '0'),
        yearInputString = this.year.toString()
    )
}

internal fun LocalTime.toSegmentedTimeState(isTwentyFourHourTimeFormat: Boolean): SegmentedTimeState {
    val displayHour = if (isTwentyFourHourTimeFormat) {
        this.hour
    } else {
        when {
            this.hour == 0 -> 12
            this.hour > 12 -> this.hour - 12
            else -> this.hour
        }
    }

    val currentMeridiemIndicator =
        if (this.hour < 12) MeridiemIndicator.AnteMeridiem else MeridiemIndicator.PostMeridiem

    return SegmentedTimeState(
        hourInputString = displayHour.toString().padStart(2, '0'),
        minuteInputString = this.minute.toString().padStart(2, '0'),
        meridiemIndicator = currentMeridiemIndicator
    )
}

internal fun MeridiemIndicator.toggle(): MeridiemIndicator {
    return if (this == MeridiemIndicator.AnteMeridiem) MeridiemIndicator.PostMeridiem else MeridiemIndicator.AnteMeridiem
}

fun reduceListingEditor(state: ListingEditorState, intent: ListingEditorIntent): ListingEditorState {
    return when (intent) {
        is ListingEditorIntent.LoadData -> {
            intent.listing.toEditorState().recalculateValidation()
        }

        is ListingEditorIntent.RestoreDraft -> {
            val restoredCurrency = SupportedCurrency.fromCode(intent.draft.priceCurrencyCode) ?: SupportedCurrency.USD

            val restoredStartDate = intent.draft.startDate?.toSegmentedDateState() ?: SegmentedDateState()
            val restoredStartTime =
                intent.draft.startTime?.toSegmentedTimeState(state.isTwentyFourHourTimeFormat) ?: SegmentedTimeState()
            val restoredEndDate = intent.draft.endDate?.toSegmentedDateState() ?: SegmentedDateState()
            val restoredEndTime =
                intent.draft.endTime?.toSegmentedTimeState(state.isTwentyFourHourTimeFormat) ?: SegmentedTimeState()

            state.copy(
                title = intent.draft.title,
                description = intent.draft.description,
                priceAmount = intent.draft.priceAmount,
                priceCurrency = restoredCurrency,
                imageUrl = intent.draft.imageUrl,
                startDate = restoredStartDate,
                startTime = restoredStartTime,
                endDate = restoredEndDate,
                endTime = restoredEndTime,
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

        is ListingEditorIntent.UpdateStartDateMonth -> {
            val updatedDate = state.startDate.copy(monthInputString = intent.monthString.filterDigitsAndTruncate(2))
            state.copy(startDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateStartDateDay -> {
            val updatedDate = state.startDate.copy(dayInputString = intent.dayString.filterDigitsAndTruncate(2))
            state.copy(startDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateStartDateYear -> {
            val updatedDate = state.startDate.copy(yearInputString = intent.yearString.filterDigitsAndTruncate(4))
            state.copy(startDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateStartTimeHour -> {
            val updatedTime = state.startTime.copy(hourInputString = intent.hourString.filterDigitsAndTruncate(2))
            state.copy(startTime = updatedTime).recalculateValidation()
        }

        is ListingEditorIntent.UpdateStartTimeMinute -> {
            val updatedTime = state.startTime.copy(minuteInputString = intent.minuteString.filterDigitsAndTruncate(2))
            state.copy(startTime = updatedTime).recalculateValidation()
        }

        ListingEditorIntent.ToggleStartTimeMeridiem -> {
            val updatedTime = state.startTime.copy(meridiemIndicator = state.startTime.meridiemIndicator.toggle())
            state.copy(startTime = updatedTime).recalculateValidation()
        }

        is ListingEditorIntent.UpdateEndDateMonth -> {
            val updatedDate = state.endDate.copy(monthInputString = intent.monthString.filterDigitsAndTruncate(2))
            state.copy(endDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateEndDateDay -> {
            val updatedDate = state.endDate.copy(dayInputString = intent.dayString.filterDigitsAndTruncate(2))
            state.copy(endDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateEndDateYear -> {
            val updatedDate = state.endDate.copy(yearInputString = intent.yearString.filterDigitsAndTruncate(4))
            state.copy(endDate = updatedDate).recalculateValidation()
        }

        is ListingEditorIntent.UpdateEndTimeHour -> {
            val updatedTime = state.endTime.copy(hourInputString = intent.hourString.filterDigitsAndTruncate(2))
            state.copy(endTime = updatedTime).recalculateValidation()
        }

        is ListingEditorIntent.UpdateEndTimeMinute -> {
            val updatedTime = state.endTime.copy(minuteInputString = intent.minuteString.filterDigitsAndTruncate(2))
            state.copy(endTime = updatedTime).recalculateValidation()
        }

        ListingEditorIntent.ToggleEndTimeMeridiem -> {
            val updatedTime = state.endTime.copy(meridiemIndicator = state.endTime.meridiemIndicator.toggle())
            state.copy(endTime = updatedTime).recalculateValidation()
        }

        is ListingEditorIntent.ChangeTimeZoneIdentifier -> {
            state.copy(timeZoneIdentifier = intent.timeZoneIdentifier, error = null).recalculateValidation()
        }

        is ListingEditorIntent.SetTwentyFourHourTimeFormat -> {
            state.copy(isTwentyFourHourTimeFormat = intent.isTwentyFourHour).recalculateValidation()
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
            startDate = SegmentedDateState(),
            startTime = SegmentedTimeState(),
            endDate = SegmentedDateState(),
            endTime = SegmentedTimeState(),
            timeZoneIdentifier = TimeZoneIdentifier(TimeZone.currentSystemDefault().id),
            type = if (state.isEditing) state.type else null,
            error = null
        ).recalculateValidation()

        is ListingEditorIntent.ClearCurrentStep -> {
            val stateWithClearedStep = when (intent.config) {
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
                    startDate = SegmentedDateState(),
                    startTime = SegmentedTimeState(),
                    endDate = SegmentedDateState(),
                    endTime = SegmentedTimeState()
                )

                ListingEditorPageConfig.Pricing -> state.copy(
                    priceAmount = PriceAmount("")
                )

                ListingEditorPageConfig.Media -> state.copy(
                    imageUrl = ImageUrl("")
                )

                ListingEditorPageConfig.Review -> state
            }
            stateWithClearedStep.copy(showDiscardDraftDialog = false, error = null).recalculateValidation()
        }
    }
}

private fun ListingEditorState.recalculateValidation(): ListingEditorState {
    val isBasicsStepCurrentlyValid = this.title.isValid()
    val isPricingStepCurrentlyValid = this.priceAmount.isValid() && this.priceAmount.value.isNotBlank()

    var calculatedScheduleValidationState: ScheduleValidationState = ScheduleValidationState.Incomplete

    if (this.type == ListingCategory.Event) {
        val parsedStartDate = this.startDate.toLocalDateOrNull()
        val parsedStartTime = this.startTime.toLocalTimeOrNull(this.isTwentyFourHourTimeFormat)
        val parsedEndDate = this.endDate.toLocalDateOrNull()
        val parsedEndTime = this.endTime.toLocalTimeOrNull(this.isTwentyFourHourTimeFormat)

        if (parsedStartDate != null && parsedStartTime != null && parsedEndDate != null && parsedEndTime != null) {
            try {
                val selectedTimeZone = TimeZone.of(this.timeZoneIdentifier.value)

                val combinedStartLocalDateTime = LocalDateTime(parsedStartDate, parsedStartTime)
                val startInstantInTimeZone = combinedStartLocalDateTime.toInstant(selectedTimeZone)
                val isStartLocalDateTimeSkipped =
                    startInstantInTimeZone.toLocalDateTime(selectedTimeZone) != combinedStartLocalDateTime

                val combinedEndLocalDateTime = LocalDateTime(parsedEndDate, parsedEndTime)
                val endInstantInTimeZone = combinedEndLocalDateTime.toInstant(selectedTimeZone)
                val isEndLocalDateTimeSkipped =
                    endInstantInTimeZone.toLocalDateTime(selectedTimeZone) != combinedEndLocalDateTime

                if (isStartLocalDateTimeSkipped || isEndLocalDateTimeSkipped) {
                    calculatedScheduleValidationState = ScheduleValidationState.DaylightSavingTimeSkipped
                } else {
                    if (combinedEndLocalDateTime.date < combinedStartLocalDateTime.date) {
                        calculatedScheduleValidationState = ScheduleValidationState.DateConflict
                    } else if (combinedEndLocalDateTime.date == combinedStartLocalDateTime.date && combinedEndLocalDateTime.time <= combinedStartLocalDateTime.time) {
                        calculatedScheduleValidationState = ScheduleValidationState.TimeConflict
                    } else {
                        calculatedScheduleValidationState =
                            ScheduleValidationState.Valid(combinedStartLocalDateTime, combinedEndLocalDateTime)
                    }
                }
            } catch (exception: Exception) {
                calculatedScheduleValidationState = ScheduleValidationState.Incomplete
            }
        }
    }

    return this.copy(
        isBasicsStepValid = isBasicsStepCurrentlyValid,
        isPricingStepValid = isPricingStepCurrentlyValid,
        scheduleValidationState = calculatedScheduleValidationState
    )
}