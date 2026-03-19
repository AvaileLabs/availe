package com.availelabs.availe.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.ui.components.dialogs.DimmedDialog
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.OutlinedButton
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun DatePickerDialog(
    initialDate: LocalDate?,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit
) {
    DimmedDialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(Theme[shapes][small])
                .background(Theme[colors][card])
                .padding(20.dp)
        ) {
            var currentSelectedDate by remember { mutableStateOf(initialDate) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.dialog_date_picker_title),
                    style = Theme[textStyles][title]
                )
                Spacer(Modifier.height(20.dp))
                CalendarView(
                    modifier = Modifier.fillMaxWidth(),
                    minDate = minDate,
                    maxDate = maxDate,
                    selectedDate = currentSelectedDate,
                    onDateSelected = { newDate ->
                        currentSelectedDate = newDate
                    }
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.generic_cancel))
                    }
                    PrimaryButton(
                        onClick = {
                            val finalSelectedDate = currentSelectedDate
                            if (finalSelectedDate != null) {
                                onDateSelected(finalSelectedDate)
                            }
                            onDismissRequest()
                        },
                        enabled = currentSelectedDate != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.dialog_date_picker_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarView(
    selectedDate: LocalDate?,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var firstOfSelectedMonth by remember {
        val selected = selectedDate ?: today
        mutableStateOf(LocalDate(selected.year, selected.month, 1))
    }

    val daysOfWeek = remember { DayOfWeek.entries.map { it.name.uppercase().take(1) } }
    val firstDayOfWeek = firstOfSelectedMonth.dayOfWeek

    val canGoPrevious by derivedStateOf {
        LocalDate(minDate.year, minDate.month, 1) <= firstOfSelectedMonth.minus(1, DateTimeUnit.MONTH)
    }
    val canGoNext by derivedStateOf {
        firstOfSelectedMonth.plus(1, DateTimeUnit.MONTH) <= LocalDate(maxDate.year, maxDate.month, 1)
    }

    @Composable
    fun DateText(
        date: LocalDate,
        enabled: Boolean,
        onClick: () -> Unit,
        color: Color,
        modifier: Modifier = Modifier
    ) {
        val textColor = when {
            date == selectedDate -> Theme[colors][onAccent]
            enabled -> color
            else -> Theme[colors][onCard].copy(alpha = 0.25f)
        }

        Box(
            modifier = modifier
                .height(40.dp)
                .clip(Theme[shapes][small])
                .clickable(role = Role.Button, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (date == today) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(Theme[shapes][small])
                        .background(Theme[colors][secondary])
                )
            }
            if (date == selectedDate) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme[colors][accent])
                )
            }
            Text(
                text = date.day.toString(),
                color = textColor,
                style = Theme[textStyles][caption]
            )
        }
    }

    var componentWidth by remember { mutableStateOf(Dp.Unspecified) }
    val cellWidth by derivedStateOf { componentWidth / 7 }
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.onGloballyPositioned { layoutCoordinates ->
            with(density) {
                componentWidth = layoutCoordinates.size.width.toDp()
            }
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(Theme[shapes][small])
                    .size(36.dp)
                    .clickable(enabled = canGoPrevious) {
                        firstOfSelectedMonth = firstOfSelectedMonth.minus(1, DateTimeUnit.MONTH)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(Res.string.accessibility_picker_previous_month),
                    tint = if (canGoPrevious) Theme[colors][onCard] else Theme[colors][outline].copy(alpha = 0.2f)
                )
            }

            val monthName = firstOfSelectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
            val yearNumber = firstOfSelectedMonth.year
            Text("$monthName $yearNumber", style = Theme[textStyles][body])

            Box(
                modifier = Modifier
                    .clip(Theme[shapes][small])
                    .size(36.dp)
                    .clickable(enabled = canGoNext) {
                        firstOfSelectedMonth = firstOfSelectedMonth.plus(1, DateTimeUnit.MONTH)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = stringResource(Res.string.accessibility_picker_next_month),
                    tint = if (canGoNext) Theme[colors][onCard] else Theme[colors][outline].copy(alpha = 0.2f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayLetter ->
                Box(Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                    Text(
                        text = dayLetter,
                        color = Theme[colors][onCard].copy(alpha = 0.5f),
                        style = Theme[textStyles][caption]
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val daysToShow = remember(firstOfSelectedMonth, minDate, maxDate) {
            val remainingDaysOfPreviousMonth = firstDayOfWeek.ordinal
            val previousMonthList = if (remainingDaysOfPreviousMonth > 0) {
                val firstDay = firstOfSelectedMonth.minus(remainingDaysOfPreviousMonth, DateTimeUnit.DAY)
                val lastDay = firstOfSelectedMonth.minus(1, DateTimeUnit.DAY)
                generateSequence(firstDay) { currentDate ->
                    if (currentDate < lastDay) currentDate.plus(1, DateTimeUnit.DAY) else null
                }.toList()
            } else {
                emptyList()
            }

            val firstDayOfCurrentMonth = LocalDate(firstOfSelectedMonth.year, firstOfSelectedMonth.month, 1)
            val lastDayOfCurrentMonth = firstDayOfCurrentMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            val currentMonthList = generateSequence(firstDayOfCurrentMonth) { currentDate ->
                if (currentDate < lastDayOfCurrentMonth) currentDate.plus(1, DateTimeUnit.DAY) else null
            }.toList()

            val daysPerCalendarMonth = 42
            val nextMonthList = run {
                val firstDayOfNextMonth = firstOfSelectedMonth.plus(1, DateTimeUnit.MONTH)
                List(daysPerCalendarMonth - previousMonthList.size - currentMonthList.size) { dayIndex ->
                    firstDayOfNextMonth.plus(dayIndex, DateTimeUnit.DAY)
                }
            }

            previousMonthList + currentMonthList + nextMonthList
        }

        Column(Modifier.fillMaxWidth()) {
            daysToShow.chunked(7).forEach { daysInWeek ->
                Row {
                    daysInWeek.forEach { currentCalendarDate ->
                        val isNotInThisMonth =
                            currentCalendarDate.month != firstOfSelectedMonth.month || currentCalendarDate.year != firstOfSelectedMonth.year
                        val isEnabled = currentCalendarDate in minDate..maxDate
                        DateText(
                            date = currentCalendarDate,
                            enabled = isEnabled,
                            onClick = { onDateSelected(currentCalendarDate) },
                            modifier = Modifier.width(cellWidth),
                            color = if (isNotInThisMonth) Theme[colors][onCard].copy(alpha = 0.5f) else Theme[colors][onCard]
                        )
                    }
                }
            }
        }
    }
}