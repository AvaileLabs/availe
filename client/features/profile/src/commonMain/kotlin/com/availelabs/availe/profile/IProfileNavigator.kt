package com.availelabs.availe.profile

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId

interface IProfileNavigator {
    fun navigateBack()
    fun navigateToListing(listingId: ListingId)
    fun navigateToChat(jid: Jid)
    fun navigateToSettings()
}