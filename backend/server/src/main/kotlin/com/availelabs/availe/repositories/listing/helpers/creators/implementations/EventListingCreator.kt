@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.listing.helpers.creators.implementations

import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.repositories.listing.helpers.creators.BaseListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.EVENT_RSVPS
import com.example.jooq.tables.references.LISTING_EVENT_DETAILS
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

internal class EventListingCreator(databaseContext: DSLContext) :
    BaseListingCreator<NewListing.Event>(databaseContext, ListingTypeEnum.event) {

    override fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: NewListing.Event,
        generatedListingId: UUID
    ) {
        val startLocalTimeLocalDateTime = java.time.LocalDateTime.parse(newListing.schedule.startLocalTime.toString())
        val endLocalTimeLocalDateTime = java.time.LocalDateTime.parse(newListing.schedule.endLocalTime.toString())

        val startInstantOffsetDateTime = OffsetDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(newListing.schedule.startInstant.toEpochMilliseconds()),
            ZoneOffset.UTC
        )
        val endInstantOffsetDateTime = OffsetDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(newListing.schedule.endInstant.toEpochMilliseconds()),
            ZoneOffset.UTC
        )

        transactionalDsl.newRecord(LISTING_EVENT_DETAILS).apply {
            set(LISTING_EVENT_DETAILS.LISTING_ID, generatedListingId)
            set(LISTING_EVENT_DETAILS.START_LOCAL_TIME, startLocalTimeLocalDateTime)
            set(LISTING_EVENT_DETAILS.END_LOCAL_TIME, endLocalTimeLocalDateTime)
            set(LISTING_EVENT_DETAILS.START_INSTANT, startInstantOffsetDateTime)
            set(LISTING_EVENT_DETAILS.END_INSTANT, endInstantOffsetDateTime)
            set(LISTING_EVENT_DETAILS.TIME_ZONE_IDENTIFIER, newListing.schedule.timeZoneIdentifier.value)
        }.insert()

        if (newListing.autoRsvp) {
            transactionalDsl.insertInto(EVENT_RSVPS)
                .set(EVENT_RSVPS.USER_ID, newListing.publisher.id.value.toJavaUuid())
                .set(EVENT_RSVPS.LISTING_ID, generatedListingId)
                .execute()
        }
    }
}