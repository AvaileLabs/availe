package com.availelabs.availe

import com.availelabs.availe.domain.model.TimeZoneIdentifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

data class DraftEventBounds(
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val timeZoneIdentifier: TimeZoneIdentifier = TimeZoneIdentifier(TimeZone.currentSystemDefault().id)
)

sealed interface PendingScheduleAdjustment {
    val proposedBounds: DraftEventBounds

    data class ShiftStartDate(override val proposedBounds: DraftEventBounds) : PendingScheduleAdjustment
    data class ShiftEndDate(override val proposedBounds: DraftEventBounds) : PendingScheduleAdjustment
    data class ShiftStartTime(override val proposedBounds: DraftEventBounds) : PendingScheduleAdjustment
    data class ShiftEndTime(override val proposedBounds: DraftEventBounds) : PendingScheduleAdjustment
}