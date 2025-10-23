package com.availelabs.availe.domain.listing

sealed interface Listing : CommonListingProperties {
    data class Item(
        val data: CommonListingData
    ) : CommonListingProperties by data, Listing

    data class Event(
        val data: CommonListingData
    ) : CommonListingProperties by data, Listing

    data class Service(
        val data: CommonListingData
    ) : CommonListingProperties by data, Listing
}