package com.availelabs.availe.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class EventSchedule(
    val startLocalTime: LocalDateTime,
    val endLocalTime: LocalDateTime,
    val startInstant: Instant,
    val endInstant: Instant,
    val timeZoneIdentifier: TimeZoneIdentifier
) {
    init {
        require(endInstant > startInstant)
        require(endLocalTime >= startLocalTime)
        require(timeZoneIdentifier.value.isNotBlank())
    }
}