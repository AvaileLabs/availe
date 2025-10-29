package com.availelabs.availe.repositories.listing.helpers.creators.implementations

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.repositories.listing.helpers.creators.BaseListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTING_EVENT_DETAILS
import org.jooq.DSLContext
import java.util.*

internal class EventListingCreator(dsl: DSLContext) :
    BaseListingCreator<NewListing.Event>(dsl, ListingTypeEnum.event) {

    override fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: NewListing.Event,
        generatedListingId: UUID
    ) {
        transactionalDsl.newRecord(LISTING_EVENT_DETAILS).apply {
            set(LISTING_EVENT_DETAILS.LISTING_ID, generatedListingId)
        }.insert()
    }
}