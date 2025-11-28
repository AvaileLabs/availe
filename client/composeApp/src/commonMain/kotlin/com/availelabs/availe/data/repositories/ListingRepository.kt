@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource
) : IListingRepository {

    override suspend fun getAllListings(): List<Listing> {
        return remoteDataSource.getAllListings()
    }

    override suspend fun getListingById(id: ListingId): FindResult<Listing> {
        return remoteDataSource.getListingById(id)
    }

    override suspend fun createListing(newListing: NewListing): InsertResult<Listing> {
        return remoteDataSource.createListing(newListing)
    }

    override suspend fun existsById(id: ListingId): Boolean {
        // Optimistic check via get
        return remoteDataSource.getListingById(id) is FindResult.Found
    }

    override suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
        return remoteDataSource.updateListing(id, patch)
    }

    override suspend fun deleteListing(id: ListingId): DeleteResult {
        return remoteDataSource.deleteListing(id)
    }
}