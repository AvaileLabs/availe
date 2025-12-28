package com.availelabs.availe.features.profilenav

import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable

@Serializable
sealed interface ProfileNavConfiguration {
    @Serializable
    data class Profile(val userId: UserId) : ProfileNavConfiguration

    @Serializable
    data class ListingDetails(val listingId: ListingId) : ProfileNavConfiguration

    @Serializable
    data object Settings : ProfileNavConfiguration
}