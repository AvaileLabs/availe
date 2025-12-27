package com.availelabs.availe.database

import com.availelabs.availe.db.SelectById
import com.availelabs.availe.db.SelectByPublisherId
import com.availelabs.availe.db.SelectPage
import com.availelabs.availe.domain.model.*

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
        imageLinks = image_links
    )
}

internal fun SelectPage.toDomain(): Listing {
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
        imageLinks = image_links
    )
}

internal fun SelectByPublisherId.toDomain(): Listing {
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
        imageLinks = image_links
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
        publisherId = publisherId,
        publisherEmail = publisherEmail
    )

    return when (listingType) {
        ListingCategory.Item -> Listing.Item(commonData, id, createdAt)
        ListingCategory.Event -> Listing.Event(commonData, id, createdAt)
        ListingCategory.Service -> Listing.Service(commonData, id, createdAt)
    }
}