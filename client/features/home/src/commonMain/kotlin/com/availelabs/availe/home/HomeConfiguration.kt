package com.availelabs.availe.home

import com.availelabs.availe.model.ListingFeedType
import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeConfiguration {
    @Serializable
    data class Feed(val feedType: ListingFeedType) : HomeConfiguration
}