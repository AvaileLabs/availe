package com.availelabs.availe.domain.extensions

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.UserId

fun Listing.canBeModifiedBy(userId: UserId): Boolean {
    return this.publisherId == userId
}