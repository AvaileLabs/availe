package com.availelabs.availe.repositories.listing.helpers.creators

import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.DbInsertResult
import com.availelabs.availe.mappers.applyCommonData
import com.availelabs.availe.mappers.toDomainListing
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTINGS
import org.jooq.DSLContext
import java.util.*

interface IListingCreator<T : NewListing> {
    fun create(newListing: T): DbInsertResult<Listing>
}

abstract class BaseListingCreator<T : NewListing>(
    private val dsl: DSLContext,
    private val listingType: ListingTypeEnum
) : IListingCreator<T> {

    override fun create(newListing: T): DbInsertResult<Listing> {
        return dsl.transactionResult { transactionConfiguration ->
            val transactionalDsl = transactionConfiguration.dsl()

            val newListingRecord = transactionalDsl.newRecord(LISTINGS)
            newListingRecord.listingType = listingType
            newListingRecord.applyCommonData(newListing.commonData)
            newListingRecord.insert()

            val generatedListingId = newListingRecord.id!!
            createSubtypeDetails(transactionalDsl, newListing, generatedListingId)

            DbInsertResult.Inserted(newListingRecord.toDomainListing())
        }
    }

    protected abstract fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: T,
        generatedListingId: UUID
    )
}