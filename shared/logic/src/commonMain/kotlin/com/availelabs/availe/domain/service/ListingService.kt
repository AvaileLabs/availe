@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.service

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

class ListingService(
    private val listingRepository: IListingRepository
) : IListingService {
    override suspend fun createListing(newListing: NewListing): InsertResult<Listing> {
        // Since we unified types, we just pass through.
        // If repo returned DbInsertResult, we would map here.
        return listingRepository.createListing(newListing)
    }

    override suspend fun getListingById(id: ListingId): FindResult<Listing> {
        return listingRepository.getListingById(id)
    }

    override suspend fun getAllListings(): List<Listing> = listingRepository.getAllListings()

    override suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing> {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return UpdateResult.NotFound
        }

        if (listing.publisherId != requestingPublisherId) {
            return UpdateResult.Forbidden
        }

        return listingRepository.updateListing(listingId, patch)
    }

    override suspend fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return DeleteResult.NotFound
        }

        if (listing.publisherId != requestingPublisherId) {
            return DeleteResult.Forbidden
        }

        return listingRepository.deleteListing(listingId)
    }
}