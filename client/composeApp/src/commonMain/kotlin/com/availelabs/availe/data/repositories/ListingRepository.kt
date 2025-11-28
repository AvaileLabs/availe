@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

interface IListingRepository {
    suspend fun getListings(): List<Listing>
    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun createListing(listing: NewListing, publisherId: UserId): InsertResult<Listing>
    suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        publisherId: UserId
    ): UpdateResult<Listing>

    suspend fun deleteListing(listingId: ListingId, publisherId: UserId): DeleteResult
}

class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource
) : IListingRepository {

    override suspend fun getListings(): List<Listing> {
        return remoteDataSource.getListings()
    }

    override suspend fun getListingById(id: ListingId): FindResult<Listing> {
        return remoteDataSource.getListingById(id)
    }

    override suspend fun createListing(listing: NewListing, publisherId: UserId): InsertResult<Listing> {
        return remoteDataSource.createListing(listing, publisherId)
    }

    override suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        publisherId: UserId
    ): UpdateResult<Listing> {
        return remoteDataSource.updateListing(listingId, patch, publisherId)
    }

    override suspend fun deleteListing(listingId: ListingId, publisherId: UserId): DeleteResult {
        return remoteDataSource.deleteListing(listingId, publisherId)
    }
}