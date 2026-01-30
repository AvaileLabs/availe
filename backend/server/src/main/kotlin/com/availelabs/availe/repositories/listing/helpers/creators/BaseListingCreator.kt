@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.listing.helpers.creators

import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.mappers.applyCommonData
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTINGS
import org.jooq.DSLContext
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

internal interface IListingCreator<T : NewListing> {
    fun create(newListing: T): Uuid
}

internal abstract class BaseListingCreator<T : NewListing>(
    private val dsl: DSLContext,
    private val listingType: ListingTypeEnum
) : IListingCreator<T> {

    override fun create(newListing: T): Uuid {
        return dsl.transactionResult { transactionConfiguration ->
            val transactionalDsl = transactionConfiguration.dsl()

            val newListingRecord = transactionalDsl.newRecord(LISTINGS)
            newListingRecord.listingType = listingType
            newListingRecord.applyCommonData(newListing.commonData, newListing.publisher.id)
            newListingRecord.insert()

            val generatedListingId = newListingRecord.id!!
            createSubtypeDetails(transactionalDsl, newListing, generatedListingId)

            generatedListingId.toKotlinUuid()
        }
    }

    protected abstract fun createSubtypeDetails(
        transactionalDsl: DSLContext,
        newListing: T,
        generatedListingId: UUID
    )
}