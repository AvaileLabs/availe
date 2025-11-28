@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.*
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.records.ListingsRecord
import org.jooq.Record
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import com.example.jooq.tables.ListingEventDetails as EventDetailsTable
import com.example.jooq.tables.ListingItemDetails as ItemDetailsTable
import com.example.jooq.tables.ListingServiceDetails as ServiceDetailsTable
import com.example.jooq.tables.Listings as ListingsTable

private fun Price.toPriceDtoV1(): PriceDto.V1 = PriceDto.V1(
    amount = this.amount,
    currency = this.currency
)

internal fun PriceDto.V1.toDomainPrice(): Price = Price(
    amount = this.amount,
    currency = this.currency
)

internal fun ListingsRecord.applyCommonData(commonData: CommonListingData) {
    this.publisherId = commonData.publisherId.value.toJavaUuid()
    this.title = commonData.title.value
    this.description = commonData.description?.value
    val monetaPrice = commonData.price.toMoneta()
    this.priceAmount = monetaPrice.number.numberValue(BigDecimal::class.java)
    this.priceCurrency = monetaPrice.currency.currencyCode
    this.imageLinks = commonData.imageLinks.map { it.value }.toTypedArray()
}

internal fun ListingsRecord.toDomainListing(): Listing {
    val common = CommonListingData(
        title = ListingTitle(this.title!!),
        description = this.description?.let { ListingDescription(it) },
        price = org.javamoney.moneta.Money.of(this.priceAmount, this.priceCurrency).toSharedPrice(),
        imageLinks = this.imageLinks?.filterNotNull()?.map { ImageUrl(it) } ?: emptyList(),
        publisherId = UserId(this.publisherId!!.toKotlinUuid())
    )

    val domainId = ListingId(this.id!!.toKotlinUuid())
    val domainCreatedAt = this.createdAt!!.toInstant().toKotlinInstant()

    return when (this.listingType) {
        ListingTypeEnum.item -> Listing.Item(common, domainId, domainCreatedAt)
        ListingTypeEnum.event -> Listing.Event(common, domainId, domainCreatedAt)
        ListingTypeEnum.service -> Listing.Service(common, domainId, domainCreatedAt)
        null -> throw IllegalStateException("Listing type from DB was null")
    }
}

internal fun Record.toDomainListing(
    listingsTable: ListingsTable,
    itemDetailsTable: ItemDetailsTable,
    eventDetailsTable: EventDetailsTable,
    serviceDetailsTable: ServiceDetailsTable
): Listing {
    val common = CommonListingData(
        title = ListingTitle(this[listingsTable.TITLE]!!),
        description = this[listingsTable.DESCRIPTION]?.let { ListingDescription(it) },
        price = org.javamoney.moneta.Money.of(this[listingsTable.PRICE_AMOUNT], this[listingsTable.PRICE_CURRENCY])
            .toSharedPrice(),
        imageLinks = this[listingsTable.IMAGE_LINKS]?.filterNotNull()?.map { ImageUrl(it) } ?: emptyList(),
        publisherId = UserId(this[listingsTable.PUBLISHER_ID]!!.toKotlinUuid())
    )

    val domainId = ListingId(this[listingsTable.ID]!!.toKotlinUuid())
    val domainCreatedAt = this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant()

    return when (this[listingsTable.LISTING_TYPE]) {
        ListingTypeEnum.item -> {
            Listing.Item(common, domainId, domainCreatedAt)
        }

        ListingTypeEnum.event -> {
            Listing.Event(common, domainId, domainCreatedAt)
        }

        ListingTypeEnum.service -> {
            Listing.Service(common, domainId, domainCreatedAt)
        }

        null -> throw IllegalStateException("Listing type from DB was null")
    }
}

internal fun Listing.toDataDto(): TypeV1ListingDtoData {
    val priceDto = this.price.toPriceDtoV1()
    return when (this) {
        is Listing.Event -> ListingEventDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            schemaVersion = 1
        )

        is Listing.Item -> ListingItemDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            schemaVersion = 1
        )

        is Listing.Service -> ListingServiceDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            publisherId = this.publisherId,
            schemaVersion = 1
        )
    }
}

internal fun TypeV1ListingDtoCreateRequest.toDomain(publisherId: UserId): NewListing {
    return when (this) {
        is ListingEventDtoSchema.V1.CreateRequest -> NewListing.Event(
            CommonListingData(
                title = title,
                description = description,
                price = price.toDomainPrice(),
                imageLinks = imageLinks,
                publisherId = publisherId
            )
        )

        is ListingItemDtoSchema.V1.CreateRequest -> NewListing.Item(
            CommonListingData(
                title = title,
                description = description,
                price = price.toDomainPrice(),
                imageLinks = imageLinks,
                publisherId = publisherId
            )
        )

        is ListingServiceDtoSchema.V1.CreateRequest -> NewListing.Service(
            CommonListingData(
                title = title,
                description = description,
                price = price.toDomainPrice(),
                imageLinks = imageLinks,
                publisherId = publisherId
            )
        )
    }
}