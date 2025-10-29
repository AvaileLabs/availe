@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.IListingRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IListingService {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: Uuid): FindResult<Listing>
    fun updateListing(
        listingId: Uuid,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: Uuid
    ): UpdateResult<Listing>

    fun deleteListing(listingId: Uuid, requestingPublisherId: Uuid): DeleteResult
}

class ListingService(
    private val listingRepository: IListingRepository
) : IListingService {
    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        return when (val result = listingRepository.createListing(newListing)) {
            is DbInsertResult.AlreadyExists -> InsertResult.AlreadyExists
            is DbInsertResult.Inserted -> InsertResult.Inserted(result.value)
        }
    }

    override fun getListingById(id: Uuid): FindResult<Listing> {
        return when (val result = listingRepository.getListingById(id)) {
            is DbFindResult.Found -> FindResult.Found(result.value)
            is DbFindResult.NotFound -> FindResult.NotFound
        }
    }

    override fun updateListing(
        listingId: Uuid,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: Uuid
    ): UpdateResult<Listing> {
        return when (val findResult = listingRepository.getListingById(listingId)) {
            is DbFindResult.NotFound -> UpdateResult.NotFound
            is DbFindResult.Found -> {
                if (findResult.value.publisherId != requestingPublisherId) {
                    return UpdateResult.Forbidden
                }

                when (val updateResult = listingRepository.updateListing(listingId, patch)) {
                    is DbUpdateResult.NotFound -> UpdateResult.NotFound
                    is DbUpdateResult.Updated -> UpdateResult.Updated(updateResult.value)
                }
            }
        }
    }

    override fun deleteListing(listingId: Uuid, requestingPublisherId: Uuid): DeleteResult {
        return when (val findResult = listingRepository.getListingById(listingId)) {
            is DbFindResult.NotFound -> DeleteResult.NotFound
            is DbFindResult.Found -> {
                if (findResult.value.publisherId != requestingPublisherId) {
                    return DeleteResult.Forbidden
                }

                when (listingRepository.deleteListing(listingId)) {
                    is DbDeleteResult.Deleted -> DeleteResult.Deleted
                    is DbDeleteResult.NotFound -> DeleteResult.NotFound
                }
            }
        }
    }
}