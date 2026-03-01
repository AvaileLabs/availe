package com.availelabs.availe.home

import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingId

interface IHomeNavigator {
    fun navigateToListingDetails(listingId: ListingId)
    fun navigateToCreateListing(category: ListingCategory? = null)
}