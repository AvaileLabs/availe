@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.*
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
    val domainCreatedAt = ListingCreationTime(this.createdAt!!.toInstant().toKotlinInstant())

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
    val domainCreatedAt = ListingCreationTime(this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant())

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