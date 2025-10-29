package com.availelabs.availe.repositories.listing.helpers.creators.implementations

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.repositories.listing.helpers.creators.BaseListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTING_ITEM_DETAILS
import org.jooq.DSLContext
import java.util.*

internal class ItemListingCreator(dsl: DSLContext) :
    BaseListingCreator<NewListing.Item>(dsl, ListingTypeEnum.item) {

    override fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: NewListing.Item,
        generatedListingId: UUID
    ) {
        transactionalDsl.newRecord(LISTING_ITEM_DETAILS).apply {
            set(LISTING_ITEM_DETAILS.LISTING_ID, generatedListingId)
        }.insert()
    }
}