package com.availelabs.availe

object ApiRoutes {
    const val LISTINGS = "listings"
}

object ApiParameters {
    const val PUBLISHER_ID = "publisherId"
}

object ApiMediaTypes {
    private const val VENDOR = "application/vnd.availe"
    const val LISTINGS_V1_JSON = "$VENDOR.listings.v1+json"
}