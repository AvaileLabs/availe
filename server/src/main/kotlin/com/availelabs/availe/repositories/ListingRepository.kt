@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories

import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.repositories.creators.IListingCreator
import org.jooq.exception.DataAccessException
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

interface IListingRepository {
    fun createListing(newListing: NewListing): InsertResult
}

class ListingRepository(
    private val itemCreator: IListingCreator<NewListing.Item>,
    private val eventCreator: IListingCreator<NewListing.Event>,
    private val serviceCreator: IListingCreator<NewListing.Service>
) : IListingRepository {

    override fun createListing(newListing: NewListing): InsertResult {
        return try {
            when (newListing) {
                is NewListing.Event -> eventCreator.create(newListing)
                is NewListing.Item -> itemCreator.create(newListing)
                is NewListing.Service -> serviceCreator.create(newListing)
            }
        } catch (e: DataAccessException) {
            throw e
        }
    }
}