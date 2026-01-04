package com.availelabs.availe.profile

import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId

interface IProfileNavigator {
    fun navigateBack()
    fun navigateToListing(listingId: ListingId)
    fun navigateToChat(userId: UserId)
    fun navigateToSettings()
}