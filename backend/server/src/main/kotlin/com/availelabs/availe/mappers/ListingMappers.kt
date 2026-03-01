package com.availelabs.availe.database

import com.availelabs.availe.db.SelectById
import com.availelabs.availe.db.SelectListings
import com.availelabs.availe.domain.model.*
import kotlin.time.Instant

fun SelectListings.toDomain(): Listing {
    val commonData = CommonListingData(
        title = this.title,
        description = this.description,
        price = Price(this.price_amount, this.price_currency),
        imageLinks = this.image_links,
        location = ListingLocation("Unknown")
    )

    val publisher = PublicProfile(
        id = this.publisher_id,
        displayName = UserDisplayName("User"),
        avatarUrl = null,
        jid = Jid(this.publisher_email.value)
    )

    val interactionState = InteractionState(
        isFavorited = this.is_favorited,
        isRsvped = this.is_rsvped
    )

    return when (this.listing_type) {
        ListingCategory.Item -> Listing.Item(commonData, this.primary_key_id, this.created_at, publisher, interactionState)
        ListingCategory.Event -> Listing.Event(commonData, this.primary_key_id, this.created_at, publisher, interactionState, this.start_time ?: Instant.fromEpochMilliseconds(0))
        ListingCategory.Service -> Listing.Service(commonData, this.primary_key_id, this.created_at, publisher, interactionState)
    }
}

fun SelectById.toDomain(): Listing {
    val commonData = CommonListingData(
        title = this.title,
        description = this.description,
        price = Price(this.price_amount, this.price_currency),
        imageLinks = this.image_links,
        location = ListingLocation("Unknown")
    )

    val publisher = PublicProfile(
        id = this.publisher_id,
        displayName = UserDisplayName("User"),
        avatarUrl = null,
        jid = Jid(this.publisher_email.value)
    )

    val interactionState = InteractionState(
        isFavorited = this.is_favorited,
        isRsvped = this.is_rsvped
    )

    return when (this.listing_type) {
        ListingCategory.Item -> Listing.Item(commonData, this.primary_key_id, this.created_at, publisher, interactionState)
        ListingCategory.Event -> Listing.Event(commonData, this.primary_key_id, this.created_at, publisher, interactionState, this.start_time ?: Instant.fromEpochMilliseconds(0))
        ListingCategory.Service -> Listing.Service(commonData, this.primary_key_id, this.created_at, publisher, interactionState)
    }
}