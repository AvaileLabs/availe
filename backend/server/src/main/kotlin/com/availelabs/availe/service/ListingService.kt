@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.service

import com.availelabs.availe.domain.extensions.canBeModifiedBy
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.IListingRepository
import kotlin.uuid.ExperimentalUuidApi

interface IListingService {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing>
    fun getAllListings(viewingUserId: UserId?): List<Listing>
    fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    ): List<Listing>

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

    override fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing> {
        return listingRepository.getListingById(id, viewingUserId)
    }

    override fun getAllListings(viewingUserId: UserId?): List<Listing> {
        return listingRepository.getAllListings(viewingUserId)
    }

    override fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing> {
        return listingRepository.getListingsPage(pageNumber, pageSize, viewingUserId, publisherId, filter)
    }

    override fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing> {
        // We fetch with viewingUserId = requestingPublisherId to ensure consistent state,
        // though for update checks simply existence matters.
        val listing = when (val result = listingRepository.getListingById(listingId, requestingPublisherId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return UpdateResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return UpdateResult.Forbidden
        }

        return listingRepository.updateListing(listingId, patch, requestingPublisherId)
    }

    override fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult {
        val listing = when (val result = listingRepository.getListingById(listingId, requestingPublisherId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return DeleteResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return DeleteResult.Forbidden
        }

        return listingRepository.deleteListing(listingId)
    }
}