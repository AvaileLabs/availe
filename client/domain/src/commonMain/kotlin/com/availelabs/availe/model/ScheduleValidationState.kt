package com.availelabs.availe.domain.model

import kotlinx.datetime.LocalDateTime

sealed interface ScheduleValidationState {
    data object Incomplete : ScheduleValidationState
    data object DateConflict : ScheduleValidationState
    data object TimeConflict : ScheduleValidationState
    data object DaylightSavingTimeSkipped : ScheduleValidationState
    data class Valid(
        val startLocalDateTime: LocalDateTime,
        val endLocalDateTime: LocalDateTime
    ) : ScheduleValidationState
}