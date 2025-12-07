package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.*
import kotlin.time.Clock

fun TypeV1ListingDtoData.toDomainListing(): Listing {
    val createdAt = ListingCreationTime(Clock.System.now())

    return when (this) {
        is ListingEventDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                publisherId = this.publisherId,
                publisherEmail = this.publisherEmail
            )
            Listing.Event(common, this.id, createdAt)
        }

        is ListingItemDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                publisherId = this.publisherId,
                publisherEmail = this.publisherEmail
            )
            Listing.Item(common, this.id, createdAt)
        }

        is ListingServiceDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                publisherId = this.publisherId,
                publisherEmail = this.publisherEmail
            )
            Listing.Service(common, this.id, createdAt)
        }
    }
}

fun Listing.toDataDto(): TypeV1ListingDtoData {
    return when (this) {
        is Listing.Event -> ListingEventDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            publisherEmail = this.publisherEmail,
            schemaVersion = 1
        )

        is Listing.Item -> ListingItemDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            publisherEmail = this.publisherEmail,
            schemaVersion = 1
        )

        is Listing.Service -> ListingServiceDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            publisherEmail = this.publisherEmail,
            schemaVersion = 1
        )
    }
}

fun NewListing.toCreateRequestDto(): TypeV1ListingDtoCreateRequest {
    return when (this) {
        is NewListing.Event -> ListingEventDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks
        )

        is NewListing.Item -> ListingItemDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks
        )

        is NewListing.Service -> ListingServiceDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks
        )
    }
}

fun TypeV1ListingDtoCreateRequest.toDomain(publisherId: UserId, publisherEmail: Email): NewListing {
    return when (this) {
        is ListingEventDtoSchema.V1.CreateRequest -> NewListing.Event(
            CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                publisherId = publisherId,
                publisherEmail = publisherEmail
            )
        )

        is ListingItemDtoSchema.V1.CreateRequest -> NewListing.Item(
            CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                publisherId = publisherId,
                publisherEmail = publisherEmail
            )
        )

        is ListingServiceDtoSchema.V1.CreateRequest -> NewListing.Service(
            CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                publisherId = publisherId,
                publisherEmail = publisherEmail
            )
        )
    }
}