@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.listing.helpers

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.mappers.applyCommonData
import com.availelabs.availe.mappers.toDomainListing
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTINGS
import org.jooq.DSLContext
import kotlin.uuid.ExperimentalUuidApi

interface IListingCreator<T : NewListing> {
    fun create(newListing: T): InsertResult
}

class ItemListingCreator(private val dsl: DSLContext) : IListingCreator<NewListing.Item> {

    override fun create(newListing: NewListing.Item): InsertResult {
        val newRecord = dsl.newRecord(LISTINGS)

        newRecord.listingType = ListingTypeEnum.item
        newRecord.applyCommonData(newListing.commonData)

        newRecord.insert()

        return InsertResult.Inserted(newRecord.toDomainListing())
    }
}

class EventListingCreator(private val dsl: DSLContext) : IListingCreator<NewListing.Event> {

    override fun create(newListing: NewListing.Event): InsertResult {
        val newRecord = dsl.newRecord(LISTINGS)

        newRecord.listingType = ListingTypeEnum.event
        newRecord.applyCommonData(newListing.commonData)

        newRecord.insert()

        return InsertResult.Inserted(newRecord.toDomainListing())
    }
}

class ServiceListingCreator(private val dsl: DSLContext) : IListingCreator<NewListing.Service> {

    override fun create(newListing: NewListing.Service): InsertResult {
        val newRecord = dsl.newRecord(LISTINGS)

        newRecord.listingType = ListingTypeEnum.service
        newRecord.applyCommonData(newListing.commonData)

        newRecord.insert()

        return InsertResult.Inserted(newRecord.toDomainListing())
    }
}