package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.network.executeSafeNetworkRequest
import com.availelabs.availe.data.sources.local.IListingLocalDataSource
import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingStoreKey
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

interface IListingRepository {
    fun getListingsPageStream(request: StoreReadRequest<ListingStoreKey>): Flow<StoreReadResponse<List<Listing>>>
    suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>>
    suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean>
    suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>>
    suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>>

    suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult>
}

class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    storeFactory: ListingStoreFactory
) : IListingRepository {

    private val store = storeFactory.create()

    override fun getListingsPageStream(request: StoreReadRequest<ListingStoreKey>): Flow<StoreReadResponse<List<Listing>>> {
        return store.stream(request)
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