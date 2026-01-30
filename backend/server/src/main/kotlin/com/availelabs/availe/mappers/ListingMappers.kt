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
import com.example.jooq.tables.Users as UsersTable

internal fun ListingsRecord.applyCommonData(commonData: CommonListingData, publisherId: UserId) {
    this.publisherId = publisherId.value.toJavaUuid()
    this.title = commonData.title.value
    this.description = commonData.description?.value
    val monetaPrice = commonData.price.toMoneta()
    this.priceAmount = monetaPrice.number.numberValue(BigDecimal::class.java)
    this.priceCurrency = monetaPrice.currency.currencyCode
    this.imageLinks = commonData.imageLinks.map { it.value }.toTypedArray()
    this.location = commonData.location.value
}

internal fun Record.toDomainListing(
    listingsTable: ListingsTable,
    usersTable: UsersTable,
    itemDetailsTable: ItemDetailsTable,
    eventDetailsTable: EventDetailsTable,
    serviceDetailsTable: ServiceDetailsTable
): Listing {
    val publisher = PublicProfile(
        id = UserId(this[usersTable.ID]!!.toKotlinUuid()),
        displayName = UserDisplayName(this[usersTable.DISPLAY_NAME]!!),
        avatarUrl = this[usersTable.AVATAR_URL]?.let { ImageUrl(it) },
        jid = Jid(this[usersTable.JID]!!)
    )

    val interactionState = InteractionState(
        isFavorited = this.get("is_favorited", Boolean::class.java) ?: false
    )

    val common = CommonListingData(
        title = ListingTitle(this[listingsTable.TITLE]!!),
        description = this[listingsTable.DESCRIPTION]?.let { ListingDescription(it) },
        price = org.javamoney.moneta.Money.of(this[listingsTable.PRICE_AMOUNT], this[listingsTable.PRICE_CURRENCY])
            .toSharedPrice(),
        imageLinks = this[listingsTable.IMAGE_LINKS]?.filterNotNull()?.map { ImageUrl(it) } ?: emptyList(),
        location = ListingLocation(this[listingsTable.LOCATION]!!)
    )

    val domainId = ListingId(this[listingsTable.ID]!!.toKotlinUuid())
    val domainCreatedAt = ListingCreationTime(this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant())

    return when (this[listingsTable.LISTING_TYPE]) {
        ListingTypeEnum.item -> {
            Listing.Item(common, domainId, domainCreatedAt, publisher, interactionState)
        }

        ListingTypeEnum.event -> {
            Listing.Event(common, domainId, domainCreatedAt, publisher, interactionState)
        }

        ListingTypeEnum.service -> {
            Listing.Service(common, domainId, domainCreatedAt, publisher, interactionState)
        }

        null -> throw IllegalStateException("Listing type from DB was null")
    }
}