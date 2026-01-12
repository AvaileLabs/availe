package com.availelabs.availe.model

import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingCategoryFilter
import kotlinx.serialization.Serializable

@Serializable
sealed interface ListingFeedType {
    @Serializable
    data object Latest : ListingFeedType

    @Serializable
    data class Category(val category: ListingCategory) : ListingFeedType
}

fun ListingFeedType.toListingCategoryFilter(): ListingCategoryFilter {
    return when (this) {
        is ListingFeedType.Latest -> ListingCategoryFilter.Companion.All
        is ListingFeedType.Category -> ListingCategoryFilter.Companion.of(this.category)
    }
}