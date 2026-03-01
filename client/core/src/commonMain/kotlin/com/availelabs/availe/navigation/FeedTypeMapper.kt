package com.availelabs.availe.navigation

import com.availelabs.availe.model.ListingFeedType

object FeedTypeMapper {
    private const val SEGMENT_GOING = "going"
    private const val SEGMENT_PAST = "past"

    fun toPathSegment(type: ListingFeedType): String {
        return when (type) {
            is ListingFeedType.Upcoming -> ""
            is ListingFeedType.Going -> SEGMENT_GOING
            is ListingFeedType.Past -> SEGMENT_PAST
        }
    }

    fun fromPathSegment(segment: String): ListingFeedType {
        return when (segment) {
            SEGMENT_GOING -> ListingFeedType.Going
            SEGMENT_PAST -> ListingFeedType.Past
            else -> ListingFeedType.Upcoming
        }
    }
}