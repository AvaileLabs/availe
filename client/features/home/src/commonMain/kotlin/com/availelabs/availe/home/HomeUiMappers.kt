package com.availelabs.availe.home

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.model.ListingFeedType
import org.jetbrains.compose.resources.StringResource

fun ListingFeedType.toUiTitle(): StringResource {
    return when (this) {
        is ListingFeedType.Latest -> Res.string.feed_latest
        is ListingFeedType.Category -> when (this.category) {
            ListingCategory.Item -> Res.string.feed_category_items
            ListingCategory.Event -> Res.string.feed_category_events
            ListingCategory.Service -> Res.string.feed_category_services
        }
    }
}