package com.availelabs.availe.repositories.listing.helpers.creators.implementations

import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.repositories.listing.helpers.creators.BaseListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTING_EVENT_DETAILS
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

internal class EventListingCreator(dsl: DSLContext) :
    BaseListingCreator<NewListing.Event>(dsl, ListingTypeEnum.event) {

    override fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: NewListing.Event,
        generatedListingId: UUID
    ) {
        val startTimeOffsetDateTime = OffsetDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(newListing.startTime.toEpochMilliseconds()),
            ZoneId.of("UTC")
        )

        transactionalDsl.newRecord(LISTING_EVENT_DETAILS).apply {
            set(LISTING_EVENT_DETAILS.LISTING_ID, generatedListingId)
            set(LISTING_EVENT_DETAILS.START_TIME, startTimeOffsetDateTime)
        }.insert()
    }
}