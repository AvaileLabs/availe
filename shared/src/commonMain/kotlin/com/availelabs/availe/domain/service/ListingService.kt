@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.service

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        return when (val result = listingRepository.updateListing(listingId, patch, requestingPublisherId)) {
            is DbUpdateResult.Updated -> UpdateResult.Updated(result.value)
            is DbUpdateResult.NotFound -> {
                if (listingRepository.existsById(listingId)) {
                    UpdateResult.Forbidden
                } else {
                    UpdateResult.NotFound
                }
            }
        }
    }

    override fun deleteListing(listingId: Uuid, requestingPublisherId: Uuid): DeleteResult {
        return when (listingRepository.deleteListing(listingId, requestingPublisherId)) {
            is DbDeleteResult.Deleted -> DeleteResult.Deleted
            is DbDeleteResult.NotFound -> {
                if (listingRepository.existsById(listingId)) {
                    DeleteResult.Forbidden
                } else {
                    DeleteResult.NotFound
                }
            }
        }
    }
}