package com.availelabs.availe.profilenav

import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface ProfileNavConfiguration {
    @OptIn(ExperimentalUuidApi::class)
    fun toUrlPath(): String

    @Serializable
    data class Profile(val userId: UserId) : ProfileNavConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = userId.value.toString()
    }

    @Serializable
    data class ListingDetails(val listingId: ListingId) : ProfileNavConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = "listing/${listingId.value}"
    }

    @Serializable
    data object Settings : ProfileNavConfiguration {
        override fun toUrlPath(): String = "settings"
    }
}