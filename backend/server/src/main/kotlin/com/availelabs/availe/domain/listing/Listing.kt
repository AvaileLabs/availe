@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.domain.listing

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal sealed interface Listing : CommonListingProperties {
    val id: Uuid
    val createdAt: Instant
    val commonData: CommonListingData

    data class Item(
        override val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing

    data class Event(
        override val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing

    data class Service(
        override val commonData: CommonListingData, override val id: Uuid, override val createdAt: Instant
    ) : CommonListingProperties by commonData, Listing
}

internal sealed interface NewListing : CommonListingProperties {
    val commonData: CommonListingData

    data class Item(
        override val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing

    data class Event(
        override val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing

    data class Service(
        override val commonData: CommonListingData
    ) : CommonListingProperties by commonData, NewListing
}