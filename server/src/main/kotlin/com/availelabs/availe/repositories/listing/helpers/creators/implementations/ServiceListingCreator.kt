package com.availelabs.availe.repositories.listing.helpers.creators.implementations

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.repositories.listing.helpers.creators.BaseListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTING_SERVICE_DETAILS
import org.jooq.DSLContext
import java.util.*

class ServiceListingCreator(dsl: DSLContext) :
    BaseListingCreator<NewListing.Service>(dsl, ListingTypeEnum.service) {

    override fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: NewListing.Service,
        generatedListingId: UUID
    ) {
        transactionalDsl.newRecord(LISTING_SERVICE_DETAILS).apply {
            set(LISTING_SERVICE_DETAILS.LISTING_ID, generatedListingId)
        }.insert()
    }
}