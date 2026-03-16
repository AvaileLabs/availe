package com.availelabs.availe.domain.model

import kotlin.time.Instant

sealed interface LocalTimeResolution {
    data class Unambiguous(val instant: Instant) : LocalTimeResolution
    data class Ambiguous(val earlierInstant: Instant, val laterInstant: Instant) : LocalTimeResolution
    data object Skipped : LocalTimeResolution
}