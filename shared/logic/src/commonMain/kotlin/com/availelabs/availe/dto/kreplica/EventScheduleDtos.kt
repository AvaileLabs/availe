package com.availelabs.availe.dto.kreplica

import com.availelabs.availe.domain.model.TimeZoneIdentifier
import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.DtoVisibility
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
sealed interface TypeV1EventScheduleDto

interface EventScheduleDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        visibility = DtoVisibility.PUBLIC
    )
    interface V1 : EventScheduleDto {
        val startLocalTime: LocalDateTime
        val endLocalTime: LocalDateTime
        val startInstant: Instant
        val endInstant: Instant
        val timeZoneIdentifier: TimeZoneIdentifier
    }
}