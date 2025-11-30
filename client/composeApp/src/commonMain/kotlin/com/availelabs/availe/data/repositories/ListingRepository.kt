package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.network.executeSafeNetworkRequest
import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.repository.IClientListingRepository
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest

class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource
) : IClientListingRepository {

    override suspend fun getAllListings(): RepositoryOperationResult<List<Listing>> {
        return executeSafeNetworkRequest {
            remoteDataSource.getAllListings()
        }
    }

    override suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>> {
        return executeSafeNetworkRequest {
            remoteDataSource.getListingById(id)
        }
    }

    override suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>> {
        return executeSafeNetworkRequest {
            remoteDataSource.createListing(newListing)
        }
    }

    override suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean> {
        return executeSafeNetworkRequest {
            remoteDataSource.existsById(id)
        }
    }

    override suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>> {
        return executeSafeNetworkRequest {
            remoteDataSource.updateListing(id, patch)
        }
    }

    override suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult> {
        return executeSafeNetworkRequest {
            remoteDataSource.deleteListing(id)
        }
    }
}