@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.domain.model

import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

sealed interface CommonListingProperties {
    val title: ListingTitle
    val description: ListingDescription?
    val price: Price
    val imageLinks: List<ImageUrl>
    val location: ListingLocation
}

data class CommonListingData(
    override val title: ListingTitle,
    override val description: ListingDescription?,
    override val price: Price,
    override val imageLinks: List<ImageUrl>,
    override val location: ListingLocation
) : CommonListingProperties

sealed interface Listing : CommonListingProperties {
    val id: ListingId
    val createdAt: ListingCreationTime
    val commonData: CommonListingData
    val publisher: PublicProfile
    val interactionState: InteractionState

    data class Item(
        override val commonData: CommonListingData,
        override val id: ListingId,
        override val createdAt: ListingCreationTime,
        override val publisher: PublicProfile,
        override val interactionState: InteractionState
    ) : CommonListingProperties by commonData, Listing

    data class Event(
        override val commonData: CommonListingData,
        override val id: ListingId,
        override val createdAt: ListingCreationTime,
        override val publisher: PublicProfile,
        override val interactionState: InteractionState
    ) : CommonListingProperties by commonData, Listing

    data class Service(
        override val commonData: CommonListingData,
        override val id: ListingId,
        override val createdAt: ListingCreationTime,
        override val publisher: PublicProfile,
        override val interactionState: InteractionState
    ) : CommonListingProperties by commonData, Listing
}

sealed interface NewListing : CommonListingProperties {
    val commonData: CommonListingData
    val publisher: PublicProfile

    data class Item(
        override val commonData: CommonListingData,
        override val publisher: PublicProfile
    ) : CommonListingProperties by commonData, NewListing

    data class Event(
        override val commonData: CommonListingData,
        override val publisher: PublicProfile
    ) : CommonListingProperties by commonData, NewListing

    data class Service(
        override val commonData: CommonListingData,
        override val publisher: PublicProfile
    ) : CommonListingProperties by commonData, NewListing
}