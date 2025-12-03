package com.availelabs.availe

object ApiRoutes {
    const val LISTINGS = "listings"
}

object ApiParameters {
    const val LISTING_ID = "listingId"
    const val PAGE_NUMBER = "page"
    const val PAGE_SIZE = "size"
}

object ApiMediaTypes {
    private const val VENDOR = "application/vnd.availe"
    const val LISTINGS_V1_JSON = "$VENDOR.listings.v1+json"
}