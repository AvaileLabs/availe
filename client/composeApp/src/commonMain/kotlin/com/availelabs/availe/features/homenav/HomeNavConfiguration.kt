package com.availelabs.availe.features.homenav

import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface HomeNavConfiguration {
    @OptIn(ExperimentalUuidApi::class)
    fun toUrlPath(): String

    @Serializable
    data object HomeFeed : HomeNavConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = ""
    }

    @Serializable
    data class ListingDetails(val listingId: ListingId) : HomeNavConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = "listing/${listingId.value}"
    }

    @Serializable
    data class Profile(val userId: UserId) : HomeNavConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = "profile/${userId.value}"
    }
}