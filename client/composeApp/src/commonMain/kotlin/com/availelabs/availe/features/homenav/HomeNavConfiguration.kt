package com.availelabs.availe.features.homenav

import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeNavConfiguration {
    @Serializable
    data object HomeFeed : HomeNavConfiguration

    @Serializable
    data class ListingDetails(val listingId: ListingId) : HomeNavConfiguration

    @Serializable
    data class Profile(val userId: UserId) : HomeNavConfiguration
}