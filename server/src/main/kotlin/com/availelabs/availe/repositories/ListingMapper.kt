@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.records.ListingsRecord
import org.javamoney.moneta.Money
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

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