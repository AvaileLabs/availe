package com.availelabs.availe.model

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface EventFeedQuery {
    data class Upcoming(val tickingNow: Flow<Instant>) : EventFeedQuery
    data class Past(val tickingNow: Flow<Instant>) : EventFeedQuery
    data object Going : EventFeedQuery
    data class Agenda(val tickingNow: Flow<Instant>, val cutoffDuration: Duration) : EventFeedQuery
}