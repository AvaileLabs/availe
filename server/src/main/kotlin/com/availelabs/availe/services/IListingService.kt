package com.availelabs.availe.services

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.InsertResult

private interface IListingService {
    fun createListing(newListing: NewListing): InsertResult
}