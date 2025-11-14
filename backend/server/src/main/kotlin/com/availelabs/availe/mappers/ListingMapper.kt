@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.mappers

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.dto.PriceDto
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.records.ListingsRecord
import org.javamoney.moneta.Money
import org.jooq.Record
import java.math.BigDecimal
import javax.money.MonetaryAmount
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
    this.publisherId = commonData.publisherId.toJavaUuid()
    this.title = commonData.title
    this.description = commonData.description
    this.priceAmount = commonData.price.number.numberValue(BigDecimal::class.java)
    this.priceCurrency = commonData.price.currency.currencyCode
    this.imageLinks = commonData.imageLinks.toTypedArray()
}

internal fun ListingsRecord.toDomainListing(): Listing {
    val common = CommonListingData(
        title = this.title!!,
        description = this.description,
        price = Money.of(this.priceAmount, this.priceCurrency),
        imageLinks = this.imageLinks?.filterNotNull() ?: emptyList(),
        publisherId = this.publisherId!!.toKotlinUuid()
    )

    val domainId = this.id!!.toKotlinUuid()
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
        title = this[listingsTable.TITLE]!!,
        description = this[listingsTable.DESCRIPTION],
        price = Money.of(this[listingsTable.PRICE_AMOUNT], this[listingsTable.PRICE_CURRENCY]),
        imageLinks = this[listingsTable.IMAGE_LINKS]?.filterNotNull() ?: emptyList(),
        publisherId = this[listingsTable.PUBLISHER_ID]!!.toKotlinUuid()
    )

    val domainId = this[listingsTable.ID]!!.toKotlinUuid()
    val domainCreatedAt = this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant()

    return when (this[listingsTable.LISTING_TYPE]) {
        ListingTypeEnum.item -> {
            // TODO: Add item-specific data extraction here
            Listing.Item(common, domainId, domainCreatedAt)
        }

        ListingTypeEnum.event -> {
            // TODO: Add event-specific data extraction here
            Listing.Event(common, domainId, domainCreatedAt)
        }

        ListingTypeEnum.service -> {
            // TODO: Add service-specific data extraction here
            Listing.Service(common, domainId, domainCreatedAt)
        }

        null -> throw IllegalStateException("Listing type from DB was null")
    }
}

internal fun PriceDto.V1.toMonetaryAmount(): MonetaryAmount = Money.parse("$currency $amount")