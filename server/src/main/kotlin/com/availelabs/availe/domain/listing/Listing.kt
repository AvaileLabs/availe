@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.domain.listing

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface Listing : CommonListingProperties {
    val id: Uuid
    val createdAt: Instant

    data class Item(
        val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing

    data class Event(
        val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing

    data class Service(
        val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing
}

sealed interface NewListing : CommonListingProperties {
    data class Item(
        val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing

    data class Event(
        val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing

    data class Service(
        val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing
}