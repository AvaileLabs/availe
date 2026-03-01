package com.availelabs.availe.model

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingCategoryFilter
import kotlinx.serialization.Serializable

@Serializable
sealed interface ListingFeedType {
    @Serializable
    data object Upcoming : ListingFeedType

    @Serializable
    data object Going : ListingFeedType

    @Serializable
    data object Past : ListingFeedType
}

fun ListingFeedType.toListingCategoryFilter(): ListingCategoryFilter {
    return ListingCategoryFilter.of(ListingCategory.Event)
}

fun ListingFeedType.toEventFeedFilter(): EventFeedFilter {
    return when (this) {
        is ListingFeedType.Upcoming -> EventFeedFilter.Upcoming
        is ListingFeedType.Going -> EventFeedFilter.Going
        is ListingFeedType.Past -> EventFeedFilter.Past
    }
}