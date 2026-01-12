package com.availelabs.availe.navigation

import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.model.ListingFeedType

object FeedTypeMapper {
    private const val SEGMENT_ITEMS = "items"
    private const val SEGMENT_EVENTS = "events"
    private const val SEGMENT_SERVICES = "services"

    fun toPathSegment(type: ListingFeedType): String {
        return when (type) {
            is ListingFeedType.Latest -> ""
            is ListingFeedType.Category -> when (type.category) {
                ListingCategory.Item -> SEGMENT_ITEMS
                ListingCategory.Event -> SEGMENT_EVENTS
                ListingCategory.Service -> SEGMENT_SERVICES
            }
        }
    }

    fun fromPathSegment(segment: String): ListingFeedType {
        return when (segment) {
            SEGMENT_ITEMS -> ListingFeedType.Category(ListingCategory.Item)
            SEGMENT_EVENTS -> ListingFeedType.Category(ListingCategory.Event)
            SEGMENT_SERVICES -> ListingFeedType.Category(ListingCategory.Service)
            else -> ListingFeedType.Latest
        }
    }
}