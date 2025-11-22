@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.domain.model.CommonListingData
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

internal fun PriceDto.V1.toDomainPrice(): Price = Price(
    amount = this.amount,
    currency = this.currency
)

internal fun Price.toDto(): PriceDto.V1 = PriceDto.V1(
    amount = this.amount,
    currency = this.currency
)

internal fun NewListing.toCreateRequestDto(): TypeV1ListingDtoCreateRequest {
    return when (this) {
        is NewListing.Event -> ListingEventDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price.toDto(),
            imageLinks = this.imageLinks
        )

        is NewListing.Item -> ListingItemDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price.toDto(),
            imageLinks = this.imageLinks
        )

        is NewListing.Service -> ListingServiceDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price.toDto(),
            imageLinks = this.imageLinks
        )
    }
}

internal fun TypeV1ListingDtoData.toDomainListing(): Listing {
    val createdAt = Instant.DISTANT_PAST

    return when (this) {
        is ListingEventDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = this.publisherId
            )
            Listing.Event(common, this.id, createdAt)
        }

        is ListingItemDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = this.publisherId
            )
            Listing.Item(common, this.id, createdAt)
        }

        is ListingServiceDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = this.publisherId
            )
            Listing.Service(common, this.id, createdAt)
        }
    }
}