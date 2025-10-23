package com.availelabs.availe

const val SERVER_PORT = 8080

object ApiRoutes {
    const val LISTINGS = "listings"
}

object ApiMediaTypes {
    private const val VENDOR = "application/vnd.availe"
    const val LISTINGS_V1_JSON = "$VENDOR.listings.v1+json"
}