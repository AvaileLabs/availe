@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

internal fun PriceDto.V1.toDomainPrice(): Price = Price(
    amount = PriceAmount(this.amount),
    currency = this.currency
)

internal fun Price.toDto(): PriceDto.V1 = PriceDto.V1(
    amount = this.amount.value,
    currency = this.currency
)

internal fun NewListing.toCreateRequestDto(): TypeV1ListingDtoCreateRequest {
    return when (this) {
        is NewListing.Event -> ListingEventDtoSchema.V1.CreateRequest(
            title = this.title.value,
            description = this.description?.value,
            price = this.price.toDto(),
            imageLinks = this.imageLinks.map { it.value }
        )

        is NewListing.Item -> ListingItemDtoSchema.V1.CreateRequest(
            title = this.title.value,
            description = this.description?.value,
            price = this.price.toDto(),
            imageLinks = this.imageLinks.map { it.value }
        )

        is NewListing.Service -> ListingServiceDtoSchema.V1.CreateRequest(
            title = this.title.value,
            description = this.description?.value,
            price = this.price.toDto(),
            imageLinks = this.imageLinks.map { it.value }
        )
    }
}

internal fun TypeV1ListingDtoData.toDomainListing(): Listing {
    val createdAt = Instant.DISTANT_PAST

    return when (this) {
        is ListingEventDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = ListingTitle(this.title),
                description = this.description?.let { ListingDescription(it) },
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks.map { ImageUrl(it) },
                publisherId = this.publisherId
            )
            Listing.Event(common, this.id, createdAt)
        }

        is ListingItemDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = ListingTitle(this.title),
                description = this.description?.let { ListingDescription(it) },
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks.map { ImageUrl(it) },
                publisherId = this.publisherId
            )
            Listing.Item(common, this.id, createdAt)
        }

        is ListingServiceDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = ListingTitle(this.title),
                description = this.description?.let { ListingDescription(it) },
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks.map { ImageUrl(it) },
                publisherId = this.publisherId
            )
            Listing.Service(common, this.id, createdAt)
        }
    }
}