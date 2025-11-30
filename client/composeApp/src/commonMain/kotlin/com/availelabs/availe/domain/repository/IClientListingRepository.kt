package com.availelabs.availe.domain.repository

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest

interface IClientListingRepository {
    suspend fun getAllListings(): RepositoryOperationResult<List<Listing>>
    suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>>
    suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean>
    suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>>
    suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>>

    suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult>
}