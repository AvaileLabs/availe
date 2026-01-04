package com.availelabs.availe.home

import com.availelabs.availe.domain.model.ListingCategory
import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeConfiguration {
    @Serializable
    data class Feed(val category: ListingCategory) : HomeConfiguration
}