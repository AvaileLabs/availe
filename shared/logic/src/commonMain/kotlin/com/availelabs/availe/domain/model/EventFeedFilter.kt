package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventFeedFilter {
    Upcoming,
    Going,
    Past,
    None
}