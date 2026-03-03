@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.CommonListingData
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.InteractionState
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingCreationTime
import com.availelabs.availe.domain.model.ListingDescription
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingLocation
import com.availelabs.availe.domain.model.ListingTitle
import com.availelabs.availe.domain.model.PublicProfile
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.domain.model.UserId
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

internal fun ListingsRecord.applyCommonData(commonListingData: CommonListingData, publisherIdentifier: UserId) {
    this.publisherId = publisherIdentifier.value.toJavaUuid()
    this.title = commonListingData.title.value
    this.description = commonListingData.description?.value
    val monetaPrice = commonListingData.price.toMoneta()
    this.priceAmount = monetaPrice.number.numberValue(BigDecimal::class.java)
    this.priceCurrency = monetaPrice.currency.currencyCode
    this.imageLinks = commonListingData.imageLinks.map { it.value }.toTypedArray()
    this.location = commonListingData.location.value
}

internal fun Record.toDomainListing(
    listingsTable: ListingsTable,
    usersTable: UsersTable,
    itemDetailsTable: ItemDetailsTable,
    eventDetailsTable: EventDetailsTable,
    serviceDetailsTable: ServiceDetailsTable
): Listing {
    val publisherProfile = PublicProfile(
        id = UserId(this[usersTable.ID]!!.toKotlinUuid()),
        displayName = UserDisplayName(this[usersTable.DISPLAY_NAME]!!),
        avatarUrl = this[usersTable.AVATAR_URL]?.let { ImageUrl(it) },
        jid = Jid(this[usersTable.JID]!!)
    )

    val interactionState = InteractionState(
        isFavorited = this.get("is_favorited", Boolean::class.javaObjectType) ?: false,
        isRsvped = this.get("is_rsvped", Boolean::class.javaObjectType) ?: false
    )

    val commonListingData = CommonListingData(
        title = ListingTitle(this[listingsTable.TITLE]!!),
        description = this[listingsTable.DESCRIPTION]?.let { ListingDescription(it) },
        price = org.javamoney.moneta.Money.of(this[listingsTable.PRICE_AMOUNT], this[listingsTable.PRICE_CURRENCY]).toSharedPrice(),
        imageLinks = this[listingsTable.IMAGE_LINKS]?.filterNotNull()?.map { ImageUrl(it) } ?: emptyList(),
        location = ListingLocation(this[listingsTable.LOCATION]!!)
    )

    val domainIdentifier = ListingId(this[listingsTable.ID]!!.toKotlinUuid())
    val domainCreationTime = ListingCreationTime(this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant())

    return when (this[listingsTable.LISTING_TYPE]) {
        ListingTypeEnum.item -> {
            Listing.Item(commonListingData, domainIdentifier, domainCreationTime, publisherProfile, interactionState)
        }

        ListingTypeEnum.event -> {
            val eventStartTime = this[eventDetailsTable.START_TIME]!!.toInstant().toKotlinInstant()
            Listing.Event(commonListingData, domainIdentifier, domainCreationTime, publisherProfile, interactionState, eventStartTime)
        }

        ListingTypeEnum.service -> {
            Listing.Service(commonListingData, domainIdentifier, domainCreationTime, publisherProfile, interactionState)
        }

        null -> throw IllegalStateException("Listing type from Database was null")
    }
}