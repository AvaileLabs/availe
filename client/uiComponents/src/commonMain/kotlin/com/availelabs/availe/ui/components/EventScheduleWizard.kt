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
import com.availelabs.availe.domain.model.LocalTimeResolution
import com.availelabs.availe.domain.model.TimeZoneIdentifier
import com.availelabs.availe.ui.components.pickers.DatePickerDialog
import com.availelabs.availe.ui.components.pickers.TimePickerDialog
import com.availelabs.availe.ui.components.pickers.TimeZoneSelectorDialog
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun EventScheduleWizard(
    startDate: LocalDate?,
    startTime: LocalTime?,
    endDate: LocalDate?,
    endTime: LocalTime?,
    timeZoneIdentifier: TimeZoneIdentifier,
    timeResolutionError: LocalTimeResolution?,
    onStartDateChanged: (LocalDate?) -> Unit,
    onStartTimeChanged: (LocalTime?) -> Unit,
    onEndDateChanged: (LocalDate?) -> Unit,
    onEndTimeChanged: (LocalTime?) -> Unit,
    onTimeZoneIdentifierChanged: (TimeZoneIdentifier) -> Unit,
    modifier: Modifier = Modifier
) {
    var isStartDatePickerVisible by remember { mutableStateOf(false) }
    var isStartTimePickerVisible by remember { mutableStateOf(false) }
    var isEndDatePickerVisible by remember { mutableStateOf(false) }
    var isEndTimePickerVisible by remember { mutableStateOf(false) }
    var isTimeZoneSelectorVisible by remember { mutableStateOf(false) }

    val currentSystemDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val maximumAllowedDate = remember { currentSystemDate.plus(5, DateTimeUnit.YEAR) }

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
                ScheduleInputBox(
                    valueText = formatLocalDateForDisplay(startDate),
                    isPlaceholder = startDate == null,
                    onClick = { isStartDatePickerVisible = true },
                    modifier = Modifier.weight(1f)
                )
                ScheduleInputBox(
                    valueText = formatLocalTimeForDisplay(startTime),
                    isPlaceholder = startTime == null,
                    onClick = { isStartTimePickerVisible = true },
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
                ScheduleInputBox(
                    valueText = formatLocalDateForDisplay(endDate),
                    isPlaceholder = endDate == null,
                    onClick = { isEndDatePickerVisible = true },
                    modifier = Modifier.weight(1f)
                )
                ScheduleInputBox(
                    valueText = formatLocalTimeForDisplay(endTime),
                    isPlaceholder = endTime == null,
                    onClick = { isEndTimePickerVisible = true },
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
            ScheduleInputBox(
                valueText = formatTimeZoneIdentifierForDisplay(timeZoneIdentifier.value),
                isPlaceholder = false,
                onClick = { isTimeZoneSelectorVisible = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (timeResolutionError is LocalTimeResolution.Skipped) {
            Text(
                text = stringResource(Res.string.listing_editor_schedule_daylight_saving_time_error),
                style = Theme[textStyles][body],
                color = Theme[colors][destructive]
            )
        }
    }

    if (isStartDatePickerVisible) {
        DatePickerDialog(
            initialDate = startDate,
            minDate = currentSystemDate,
            maxDate = maximumAllowedDate,
            onDateSelected = { newDate ->
                onStartDateChanged(newDate)
            },
            onDismissRequest = { isStartDatePickerVisible = false }
        )
    }

    if (isStartTimePickerVisible) {
        TimePickerDialog(
            initialTime = startTime,
            is24Hour = false,
            onTimeSelected = { newTime ->
                onStartTimeChanged(newTime)
            },
            onDismissRequest = { isStartTimePickerVisible = false }
        )
    }

    if (isEndDatePickerVisible) {
        DatePickerDialog(
            initialDate = endDate,
            minDate = currentSystemDate,
            maxDate = maximumAllowedDate,
            onDateSelected = { newDate ->
                onEndDateChanged(newDate)
            },
            onDismissRequest = { isEndDatePickerVisible = false }
        )
    }

    if (isEndTimePickerVisible) {
        TimePickerDialog(
            initialTime = endTime,
            is24Hour = false,
            onTimeSelected = { newTime ->
                onEndTimeChanged(newTime)
            },
            onDismissRequest = { isEndTimePickerVisible = false }
        )
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

@Composable
private fun ScheduleInputBox(
    valueText: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(Theme[shapes][medium])
            .border(1.dp, Theme[colors][outline].copy(alpha = 0.2f), Theme[shapes][medium])
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = valueText,
            style = Theme[textStyles][body],
            color = if (isPlaceholder) Theme[colors][onBackground].copy(alpha = 0.5f) else Theme[colors][onBackground]
        )
    }
}

@Composable
private fun formatLocalDateForDisplay(localDate: LocalDate?): String {
    if (localDate == null) return stringResource(Res.string.listing_editor_schedule_select_date)
    val monthNamePrefix = localDate.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val dayOfMonthString = localDate.day.toString()
    val yearString = localDate.year.toString()
    return "$monthNamePrefix $dayOfMonthString, $yearString"
}

@Composable
private fun formatLocalTimeForDisplay(localTime: LocalTime?): String {
    if (localTime == null) return stringResource(Res.string.listing_editor_schedule_select_time)
    val displayHourValue = when {
        localTime.hour == 0 -> 12
        localTime.hour > 12 -> localTime.hour - 12
        else -> localTime.hour
    }
    val periodIndicator = if (localTime.hour < 12) "AM" else "PM"
    val displayMinuteString = localTime.minute.toString().padStart(2, '0')
    return "$displayHourValue:$displayMinuteString $periodIndicator"
}

private fun formatTimeZoneIdentifierForDisplay(timeZoneIdentifierString: String): String {
    val splitComponents = timeZoneIdentifierString.split("/")
    if (splitComponents.size < 2) return timeZoneIdentifierString.replace("_", " ")
    val regionComponent = splitComponents.first()
    val cityComponent = splitComponents.drop(1).joinToString(" / ") { it.replace("_", " ") }
    return "$cityComponent ($regionComponent)"
}