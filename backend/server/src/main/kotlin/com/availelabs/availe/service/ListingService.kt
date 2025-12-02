@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.service

import com.availelabs.availe.domain.extensions.canBeModifiedBy
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.IListingRepository
import kotlin.uuid.ExperimentalUuidApi

interface IListingService {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: ListingId): FindResult<Listing>
    fun getAllListings(): List<Listing>
    fun getListingsPage(pageNumber: Int, pageSize: Int): List<Listing>
    fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing>

    fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult
}

class ListingService(
    private val listingRepository: IListingRepository
) : IListingService {
    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        return listingRepository.createListing(newListing)
    }

    override fun getListingById(id: ListingId): FindResult<Listing> {
        return listingRepository.getListingById(id)
    }

    override fun getAllListings(): List<Listing> = listingRepository.getAllListings()

    override fun getListingsPage(pageNumber: Int, pageSize: Int): List<Listing> {
        return listingRepository.getListingsPage(pageNumber, pageSize)
    }

    override fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing> {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return UpdateResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return UpdateResult.Forbidden
        }

        return listingRepository.updateListing(listingId, patch)
    }

    override fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return DeleteResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return DeleteResult.Forbidden
        }

        return listingRepository.deleteListing(listingId)
    }
}