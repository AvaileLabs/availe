package com.availelabs.availe.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.domain.model.ScheduleValidationState
import com.availelabs.availe.domain.model.TimeZoneIdentifier
import com.availelabs.availe.ui.components.pickers.TimeZoneSelectorDialog
import com.availelabs.availe.ui.model.MeridiemIndicator
import com.availelabs.availe.ui.model.SegmentedDateState
import com.availelabs.availe.ui.model.SegmentedTimeState
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun EventScheduleWizard(
    startDate: SegmentedDateState,
    startTime: SegmentedTimeState,
    endDate: SegmentedDateState,
    endTime: SegmentedTimeState,
    timeZoneIdentifier: TimeZoneIdentifier,
    isTwentyFourHourFormat: Boolean,
    scheduleValidationState: ScheduleValidationState,
    onStartDateMonthChanged: (String) -> Unit,
    onStartDateDayChanged: (String) -> Unit,
    onStartDateYearChanged: (String) -> Unit,
    onStartTimeHourChanged: (String) -> Unit,
    onStartTimeMinuteChanged: (String) -> Unit,
    onStartTimeMeridiemToggled: () -> Unit,
    onEndDateMonthChanged: (String) -> Unit,
    onEndDateDayChanged: (String) -> Unit,
    onEndDateYearChanged: (String) -> Unit,
    onEndTimeHourChanged: (String) -> Unit,
    onEndTimeMinuteChanged: (String) -> Unit,
    onEndTimeMeridiemToggled: () -> Unit,
    onTimeZoneIdentifierChanged: (TimeZoneIdentifier) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTimeZoneSelectorVisible by remember { mutableStateOf(false) }

    val hasDateConflict = scheduleValidationState is ScheduleValidationState.DateConflict
    val hasTimeConflict = scheduleValidationState is ScheduleValidationState.TimeConflict

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.listing_editor_schedule_start),
                style = Theme[textStyles][caption],
                color = Theme[colors][onBackground].copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartDateInput(
                    monthString = startDate.monthInputString,
                    dayString = startDate.dayInputString,
                    yearString = startDate.yearInputString,
                    onMonthChanged = onStartDateMonthChanged,
                    onDayChanged = onStartDateDayChanged,
                    onYearChanged = onStartDateYearChanged,
                    monthPlaceholderString = stringResource(Res.string.listing_editor_schedule_month_placeholder),
                    dayPlaceholderString = stringResource(Res.string.listing_editor_schedule_day_placeholder),
                    yearPlaceholderString = stringResource(Res.string.listing_editor_schedule_year_placeholder),
                    hasError = hasDateConflict,
                    modifier = Modifier.weight(1f)
                )

                val startMeridiemTextString = if (startTime.meridiemIndicator == MeridiemIndicator.AnteMeridiem) {
                    stringResource(Res.string.listing_editor_schedule_meridiem_am)
                } else {
                    stringResource(Res.string.listing_editor_schedule_meridiem_pm)
                }

                SmartTimeInput(
                    hourString = startTime.hourInputString,
                    minuteString = startTime.minuteInputString,
                    meridiemIndicatorString = startMeridiemTextString,
                    isTwentyFourHourFormat = isTwentyFourHourFormat,
                    onHourChanged = onStartTimeHourChanged,
                    onMinuteChanged = onStartTimeMinuteChanged,
                    onMeridiemToggled = onStartTimeMeridiemToggled,
                    hourPlaceholderString = stringResource(Res.string.listing_editor_schedule_hour_placeholder),
                    minutePlaceholderString = stringResource(Res.string.listing_editor_schedule_minute_placeholder),
                    hasError = hasTimeConflict,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.listing_editor_schedule_end),
                style = Theme[textStyles][caption],
                color = Theme[colors][onBackground].copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartDateInput(
                    monthString = endDate.monthInputString,
                    dayString = endDate.dayInputString,
                    yearString = endDate.yearInputString,
                    onMonthChanged = onEndDateMonthChanged,
                    onDayChanged = onEndDateDayChanged,
                    onYearChanged = onEndDateYearChanged,
                    monthPlaceholderString = stringResource(Res.string.listing_editor_schedule_month_placeholder),
                    dayPlaceholderString = stringResource(Res.string.listing_editor_schedule_day_placeholder),
                    yearPlaceholderString = stringResource(Res.string.listing_editor_schedule_year_placeholder),
                    hasError = hasDateConflict,
                    modifier = Modifier.weight(1f)
                )

                val endMeridiemTextString = if (endTime.meridiemIndicator == MeridiemIndicator.AnteMeridiem) {
                    stringResource(Res.string.listing_editor_schedule_meridiem_am)
                } else {
                    stringResource(Res.string.listing_editor_schedule_meridiem_pm)
                }

                SmartTimeInput(
                    hourString = endTime.hourInputString,
                    minuteString = endTime.minuteInputString,
                    meridiemIndicatorString = endMeridiemTextString,
                    isTwentyFourHourFormat = isTwentyFourHourFormat,
                    onHourChanged = onEndTimeHourChanged,
                    onMinuteChanged = onEndTimeMinuteChanged,
                    onMeridiemToggled = onEndTimeMeridiemToggled,
                    hourPlaceholderString = stringResource(Res.string.listing_editor_schedule_hour_placeholder),
                    minutePlaceholderString = stringResource(Res.string.listing_editor_schedule_minute_placeholder),
                    hasError = hasTimeConflict,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.listing_editor_schedule_time_zone),
                style = Theme[textStyles][caption],
                color = Theme[colors][onBackground].copy(alpha = 0.7f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(Theme[shapes][medium])
                    .border(1.dp, Theme[colors][outline].copy(alpha = 0.2f), Theme[shapes][medium])
                    .clickable(onClick = { isTimeZoneSelectorVisible = true })
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formatTimeZoneIdentifierForDisplay(timeZoneIdentifier.value),
                    style = Theme[textStyles][body],
                    color = Theme[colors][onBackground]
                )
            }
        }

        when (scheduleValidationState) {
            is ScheduleValidationState.DateConflict -> {
                Text(
                    text = stringResource(Res.string.listing_editor_error_date_conflict),
                    style = Theme[textStyles][body],
                    color = Theme[colors][destructive]
                )
            }

            is ScheduleValidationState.TimeConflict -> {
                Text(
                    text = stringResource(Res.string.listing_editor_error_time_conflict),
                    style = Theme[textStyles][body],
                    color = Theme[colors][destructive]
                )
            }

            is ScheduleValidationState.DaylightSavingTimeSkipped -> {
                Text(
                    text = stringResource(Res.string.listing_editor_schedule_daylight_saving_time_error),
                    style = Theme[textStyles][body],
                    color = Theme[colors][destructive]
                )
            }

            else -> {}
        }
    }

    if (isTimeZoneSelectorVisible) {
        TimeZoneSelectorDialog(
            initialZoneId = timeZoneIdentifier.value,
            onZoneSelected = { newZoneIdString ->
                onTimeZoneIdentifierChanged(TimeZoneIdentifier(newZoneIdString))
            },
            onDismissRequest = { isTimeZoneSelectorVisible = false }
        )
    }
}

private fun formatTimeZoneIdentifierForDisplay(timeZoneIdentifierString: String): String {
    val splitComponents = timeZoneIdentifierString.split("/")
    if (splitComponents.size < 2) return timeZoneIdentifierString.replace("_", " ")
    val regionComponent = splitComponents.first()
    val cityComponent = splitComponents.drop(1).joinToString(" / ") { it.replace("_", " ") }
    return "$cityComponent ($regionComponent)"
}