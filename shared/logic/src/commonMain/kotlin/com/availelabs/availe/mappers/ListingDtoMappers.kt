package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.*
import kotlin.time.Clock

fun TypeV1ListingDtoData.toDomainListing(): Listing {
    val createdAt = ListingCreationTime(Clock.System.now())

    return when (this) {
        is ListingEventDtoSchema.V1.Data -> {
            val publisherDomain = this.publisher.toDomain()
            val interactionStateDomain = this.interactionState.toDomain()

            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                location = this.location
            )
            Listing.Event(
                commonData = common,
                id = this.id,
                createdAt = createdAt,
                publisher = publisherDomain,
                interactionState = interactionStateDomain,
                startTime = this.startTime
            )
        }

        is ListingItemDtoSchema.V1.Data -> {
            val publisherDomain = this.publisher.toDomain()
            val interactionStateDomain = this.interactionState.toDomain()

            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                location = this.location
            )
            Listing.Item(
                commonData = common,
                id = this.id,
                createdAt = createdAt,
                publisher = publisherDomain,
                interactionState = interactionStateDomain
            )
        }

        is ListingServiceDtoSchema.V1.Data -> {
            val publisherDomain = this.publisher.toDomain()
            val interactionStateDomain = this.interactionState.toDomain()

            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price,
                imageLinks = this.imageLinks,
                location = this.location
            )
            Listing.Service(
                commonData = common,
                id = this.id,
                createdAt = createdAt,
                publisher = publisherDomain,
                interactionState = interactionStateDomain
            )
        }
    }
}

fun Listing.toDataDto(): TypeV1ListingDtoData {
    val publisherDto = this.publisher.toDto()
    val interactionDto = this.interactionState.toDto()

    return when (this) {
        is Listing.Event -> ListingEventDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            location = this.location,
            publisher = publisherDto,
            interactionState = interactionDto,
            startTime = this.startTime,
            schemaVersion = 1
        )

        is Listing.Item -> ListingItemDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            location = this.location,
            publisher = publisherDto,
            interactionState = interactionDto,
            schemaVersion = 1
        )

        is Listing.Service -> ListingServiceDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            location = this.location,
            publisher = publisherDto,
            interactionState = interactionDto,
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
            imageLinks = this.imageLinks,
            location = this.location,
            startTime = this.startTime
        )

        is NewListing.Item -> ListingItemDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            location = this.location
        )

        is NewListing.Service -> ListingServiceDtoSchema.V1.CreateRequest(
            title = this.title,
            description = this.description,
            price = this.price,
            imageLinks = this.imageLinks,
            location = this.location
        )
    }
}

fun TypeV1ListingDtoCreateRequest.toDomain(publisher: PublicProfile): NewListing {
    return when (this) {
        is ListingEventDtoSchema.V1.CreateRequest -> NewListing.Event(
            commonData = CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                location = location
            ),
            publisher = publisher,
            startTime = startTime
        )

        is ListingItemDtoSchema.V1.CreateRequest -> NewListing.Item(
            commonData = CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                location = location
            ),
            publisher = publisher
        )

        is ListingServiceDtoSchema.V1.CreateRequest -> NewListing.Service(
            commonData = CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                location = location
            ),
            publisher = publisher
        )
    }
}

private fun PublicProfileDtoSchema.V1.Data.toDomain(): PublicProfile {
    return PublicProfile(
        id = this.id,
        displayName = this.displayName,
        avatarUrl = this.avatarUrl,
        jid = this.jid
    )
}

private fun PublicProfile.toDto(): PublicProfileDtoSchema.V1.Data {
    return PublicProfileDtoSchema.V1.Data(
        id = this.id,
        displayName = this.displayName,
        avatarUrl = this.avatarUrl,
        jid = this.jid,
        schemaVersion = 1
    )
}

private fun InteractionStateDtoSchema.V1.Data.toDomain(): InteractionState {
    return InteractionState(
        isFavorited = this.isFavorited,
        isRsvped = this.isRsvped
    )
}

private fun InteractionState.toDto(): InteractionStateDtoSchema.V1.Data {
    return InteractionStateDtoSchema.V1.Data(
        isFavorited = this.isFavorited,
        isRsvped = this.isRsvped,
        schemaVersion = 1
    )
}