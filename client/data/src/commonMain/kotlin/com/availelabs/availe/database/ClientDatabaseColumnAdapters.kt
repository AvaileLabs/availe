package com.availelabs.availe.database

import com.availelabs.availe.db.SelectById
import com.availelabs.availe.db.SelectListings
import com.availelabs.availe.domain.model.*
import kotlin.time.Instant

internal fun SelectById.toDomain(): Listing {
    return mapRawDataToListing(
        id = primary_key_id,
        createdAt = created_at,
        publisherId = publisher_id,
        publisherEmail = publisher_email,
        listingType = listing_type,
        title = title,
        description = description,
        priceAmount = price_amount,
        priceCurrency = price_currency,
        imageLinks = image_links,
        isRsvped = is_rsvped,
        isFavorited = is_favorited,
        startTime = start_time
    )
}

internal fun SelectListings.toDomain(): Listing {
    return mapRawDataToListing(
        id = primary_key_id,
        createdAt = created_at,
        publisherId = publisher_id,
        publisherEmail = publisher_email,
        listingType = listing_type,
        title = title,
        description = description,
        priceAmount = price_amount,
        priceCurrency = price_currency,
        imageLinks = image_links,
        isRsvped = is_rsvped,
        isFavorited = is_favorited,
        startTime = start_time
    )
}

private fun mapRawDataToListing(
    id: ListingId,
    createdAt: ListingCreationTime,
    publisherId: UserId,
    publisherEmail: Email,
    listingType: ListingCategory,
    title: ListingTitle,
    description: ListingDescription?,
    priceAmount: PriceAmount,
    priceCurrency: CurrencyCode,
    imageLinks: List<ImageUrl>,
    isRsvped: Boolean,
    isFavorited: Boolean,
    startTime: Instant?
): Listing {
    val price = Price(
        amount = priceAmount,
        currency = priceCurrency
    )

    val publisher = PublicProfile(
        id = publisherId,
        displayName = UserDisplayName(publisherEmail.value.substringBefore("@").ifBlank { "User" }),
        avatarUrl = null,
        jid = Jid(publisherEmail.value)
    )

    val interactionState = InteractionState(
        isFavorited = isFavorited,
        isRsvped = isRsvped
    )

    val commonData = CommonListingData(
        title = title,
        description = description,
        price = price,
        imageLinks = imageLinks,
        location = ListingLocation("Unknown")
    )

    return when (listingType) {
        ListingCategory.Item -> Listing.Item(commonData, id, createdAt, publisher, interactionState)
        ListingCategory.Event -> Listing.Event(commonData, id, createdAt, publisher, interactionState, startTime ?: Instant.fromEpochMilliseconds(0))
        ListingCategory.Service -> Listing.Service(commonData, id, createdAt, publisher, interactionState)
    }
}