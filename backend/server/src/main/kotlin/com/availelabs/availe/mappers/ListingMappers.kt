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

    val defaultInteractionState = InteractionState(
        isFavorited = false,
        isRsvped = false
    )

    val commonListingData = CommonListingData(
        title = ListingTitle(this[listingsTable.TITLE]!!),
        description = this[listingsTable.DESCRIPTION]?.let { ListingDescription(it) },
        price = org.javamoney.moneta.Money.of(this[listingsTable.PRICE_AMOUNT], this[listingsTable.PRICE_CURRENCY])
            .toSharedPrice(),
        imageLinks = this[listingsTable.IMAGE_LINKS]?.filterNotNull()?.map { ImageUrl(it) } ?: emptyList(),
        location = ListingLocation(this[listingsTable.LOCATION]!!)
    )

    val domainIdentifier = ListingId(this[listingsTable.ID]!!.toKotlinUuid())
    val domainCreationTime = ListingCreationTime(this[listingsTable.CREATED_AT]!!.toInstant().toKotlinInstant())

    return when (this[listingsTable.LISTING_TYPE]) {
        ListingTypeEnum.item -> {
            Listing.Item(
                commonData = commonListingData,
                id = domainIdentifier,
                createdAt = domainCreationTime,
                publisher = publisherProfile,
                interactionState = defaultInteractionState
            )
        }

        ListingTypeEnum.event -> {
            val startLocalTimeDatabaseRecord = this[eventDetailsTable.START_LOCAL_TIME]!!
            val startLocalTimeKotlin = kotlinx.datetime.LocalDateTime(
                year = startLocalTimeDatabaseRecord.year,
                month = startLocalTimeDatabaseRecord.monthValue,
                day = startLocalTimeDatabaseRecord.dayOfMonth,
                hour = startLocalTimeDatabaseRecord.hour,
                minute = startLocalTimeDatabaseRecord.minute,
                second = startLocalTimeDatabaseRecord.second,
                nanosecond = startLocalTimeDatabaseRecord.nano
            )

            val endLocalTimeDatabaseRecord = this[eventDetailsTable.END_LOCAL_TIME]!!
            val endLocalTimeKotlin = kotlinx.datetime.LocalDateTime(
                year = endLocalTimeDatabaseRecord.year,
                month = endLocalTimeDatabaseRecord.monthValue,
                day = endLocalTimeDatabaseRecord.dayOfMonth,
                hour = endLocalTimeDatabaseRecord.hour,
                minute = endLocalTimeDatabaseRecord.minute,
                second = endLocalTimeDatabaseRecord.second,
                nanosecond = endLocalTimeDatabaseRecord.nano
            )

            val startInstantKotlin = this[eventDetailsTable.START_INSTANT]!!.toInstant().toKotlinInstant()
            val endInstantKotlin = this[eventDetailsTable.END_INSTANT]!!.toInstant().toKotlinInstant()
            val timeZoneIdentifierKotlin = TimeZoneIdentifier(this[eventDetailsTable.TIME_ZONE_IDENTIFIER]!!)

            val eventSchedule = EventSchedule(
                startLocalTime = startLocalTimeKotlin,
                endLocalTime = endLocalTimeKotlin,
                startInstant = startInstantKotlin,
                endInstant = endInstantKotlin,
                timeZoneIdentifier = timeZoneIdentifierKotlin
            )

            Listing.Event(
                commonData = commonListingData,
                id = domainIdentifier,
                createdAt = domainCreationTime,
                publisher = publisherProfile,
                interactionState = defaultInteractionState,
                schedule = eventSchedule
            )
        }

        ListingTypeEnum.service -> {
            Listing.Service(
                commonData = commonListingData,
                id = domainIdentifier,
                createdAt = domainCreationTime,
                publisher = publisherProfile,
                interactionState = defaultInteractionState
            )
        }

        null -> throw IllegalStateException("Listing type from Database was null")
    }
}