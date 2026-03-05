package com.availelabs.availe.listingdetails

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId

interface IListingDetailsNavigator {
    fun navigateBack()
    fun navigateToChat(jid: Jid)
    fun navigateToProfile(userId: UserId)
    fun navigateToEditListing(listingId: ListingId)
    fun navigateToLogin()
}