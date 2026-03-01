package com.availelabs.availe.home

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.model.ListingFeedType
import org.jetbrains.compose.resources.StringResource

fun ListingFeedType.toUiTitle(): StringResource {
    return when (this) {
        is ListingFeedType.Upcoming -> Res.string.feed_upcoming
        is ListingFeedType.Going -> Res.string.feed_going
        is ListingFeedType.Past -> Res.string.feed_past
    }
}