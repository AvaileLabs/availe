package com.availelabs.availe.repositories

import com.availelabs.availe.domain.listing.NewListing

interface IListingRepository {
    fun createListing(newListing: NewListing): Boolean
}