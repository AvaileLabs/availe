package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.network.executeSafeNetworkRequest
import com.availelabs.availe.data.sources.local.IListingLocalDataSource
import com.availelabs.availe.data.sources.local.IListingPreferences
import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingStoreKey
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import kotlin.time.Clock

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
    suspend fun isListingCacheStale(): Boolean
}

class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    storeFactory: ListingStoreFactory,
    private val listingPreferences: IListingPreferences
) : IListingRepository {

    private val store = storeFactory.create()

    override fun getListingsPageStream(request: StoreReadRequest<ListingStoreKey>): Flow<StoreReadResponse<List<Listing>>> {
        return store.stream(request).onEach { response ->
            if (response.origin is StoreReadResponseOrigin.Fetcher && response is StoreReadResponse.Data) {
                listingPreferences.updateLastListingsFetchTime(Clock.System.now().toEpochMilliseconds())
            }
        }
    }

    override suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>> {
        return executeSafeNetworkRequest {
            val findResult = remoteDataSource.getListingById(id)
            if (findResult is FindResult.NotFound) {
                localDataSource.deleteListingById(id)
            }
            findResult
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

    override suspend fun isListingCacheStale(): Boolean {
        if (localDataSource.isListingDatabaseEmpty()) {
            return true
        }

        val lastFetchTimeMilliseconds = listingPreferences.getLastListingsFetchTimeMilliseconds() ?: return true

        val currentTimestampMilliseconds = Clock.System.now().toEpochMilliseconds()
        val stalenessThresholdMilliseconds = 15 * 60 * 1000

        return (currentTimestampMilliseconds - lastFetchTimeMilliseconds) > stalenessThresholdMilliseconds
    }
}