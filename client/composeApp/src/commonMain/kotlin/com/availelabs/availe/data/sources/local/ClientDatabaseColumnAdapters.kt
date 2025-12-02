package com.availelabs.availe.data.sources.local

import com.availelabs.availe.db.SelectById
import com.availelabs.availe.db.SelectPage
import com.availelabs.availe.domain.model.*
import kotlin.time.Instant

fun SelectById.toDomain(): Listing {
    return mapRawDataToListing(
        id = primary_key_id,
        createdAt = created_at,
        publisherId = publisher_id,
        listingType = listing_type,
        title = title,
        description = description,
        priceAmount = price_amount,
        priceCurrency = price_currency,
        imageLinks = image_links
    )
}

fun SelectPage.toDomain(): Listing {
    return mapRawDataToListing(
        id = primary_key_id,
        createdAt = created_at,
        publisherId = publisher_id,
        listingType = listing_type,
        title = title,
        description = description,
        priceAmount = price_amount,
        priceCurrency = price_currency,
        imageLinks = image_links
    )
}

private fun mapRawDataToListing(
    id: ListingId,
    createdAt: Instant,
    publisherId: UserId,
    listingType: ListingCategory,
    title: ListingTitle,
    description: ListingDescription?,
    priceAmount: PriceAmount,
    priceCurrency: CurrencyCode,
    imageLinks: List<ImageUrl>
): Listing {
    val price = Price(
        amount = priceAmount,
        currency = priceCurrency
    )

    val commonData = CommonListingData(
        title = title,
        description = description,
        price = price,
        imageLinks = imageLinks,
        publisherId = publisherId
    )

    return when (listingType) {
        ListingCategory.Item -> Listing.Item(commonData, id, createdAt)
        ListingCategory.Event -> Listing.Event(commonData, id, createdAt)
        ListingCategory.Service -> Listing.Service(commonData, id, createdAt)
    }
}