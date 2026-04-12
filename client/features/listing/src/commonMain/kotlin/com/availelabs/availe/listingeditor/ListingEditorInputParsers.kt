package com.availelabs.availe.listingeditor

import com.availelabs.availe.ui.model.MeridiemIndicator
import com.availelabs.availe.ui.model.SegmentedDateState
import com.availelabs.availe.ui.model.SegmentedTimeState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

internal fun SegmentedDateState.toLocalDateOrNull(): LocalDate? {
    return try {
        val parsedMonth = monthInputString.toInt()
        val parsedDay = dayInputString.toInt()
        var parsedYear = yearInputString.toInt()

        if (yearInputString.length == 2) {
            parsedYear += 2000
        }

        LocalDate(year = parsedYear, month = parsedMonth, day = parsedDay)
    } catch (exception: Exception) {
        null
    }
}

internal fun SegmentedTimeState.toLocalTimeOrNull(isTwentyFourHourFormat: Boolean): LocalTime? {
    return try {
        var parsedHour = hourInputString.toInt()
        val parsedMinute = minuteInputString.toInt()

        if (!isTwentyFourHourFormat) {
            if (meridiemIndicator == MeridiemIndicator.AnteMeridiem && parsedHour == 12) {
                parsedHour = 0
            } else if (meridiemIndicator == MeridiemIndicator.PostMeridiem && parsedHour < 12) {
                parsedHour += 12
            }
        }

        LocalTime(hour = parsedHour, minute = parsedMinute)
    } catch (exception: Exception) {
        null
    }
}