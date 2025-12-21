package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.network.ISafeNetworkCaller
import com.availelabs.availe.data.preferences.IListingPreferences
import com.availelabs.availe.data.repositories.store.ListingStoreFactory
import com.availelabs.availe.data.sources.local.IListingLocalDataSource
import com.availelabs.availe.data.sources.local.ILocalDataCleaner
import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingStoreKey
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    private val listingPreferences: IListingPreferences,
    private val localDataCleaner: ILocalDataCleaner,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IListingRepository {

    private val store = storeFactory.create()

    override fun getListingsPageStream(request: StoreReadRequest<ListingStoreKey>): Flow<StoreReadResponse<List<Listing>>> {
        return store.stream(request)
            .onEach { response ->
                if (response.origin is StoreReadResponseOrigin.Fetcher && response is StoreReadResponse.Data) {
                    try {
                        listingPreferences.updateLastListingsFetchTime(Clock.System.now().toEpochMilliseconds())
                    } catch (_: Exception) {
                        // Ignore preference update failure
                    }
                }
            }
            .catch { exception ->
                throw exception
            }
    }

    override suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>> {
        return safeNetworkCaller.execute {
            val findResult = remoteDataSource.getListingById(id)
            if (findResult is FindResult.NotFound) {
                TODO()
                // Optionally clear specific item from local data source if needed,
                // but avoiding full wipe.
            }
            findResult
        }
    }

    override suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>> {
        return safeNetworkCaller.execute {
            remoteDataSource.createListing(newListing)
        }
    }

    override suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean> {
        return safeNetworkCaller.execute {
            remoteDataSource.existsById(id)
        }
    }

    override suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>> {
        return safeNetworkCaller.execute {
            remoteDataSource.updateListing(id, patch)
        }
    }

    override suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult> {
        return safeNetworkCaller.execute {
            remoteDataSource.deleteListing(id)
        }
    }

    override suspend fun isListingCacheStale(): Boolean {
        return try {
            if (localDataSource.isListingDatabaseEmpty()) {
                return true
            }

            val lastFetchTimeMilliseconds = listingPreferences.getLastListingsFetchTimeMilliseconds() ?: return true

            val currentTimestampMilliseconds = Clock.System.now().toEpochMilliseconds()
            val stalenessThresholdMilliseconds = 15 * 60 * 1000

            (currentTimestampMilliseconds - lastFetchTimeMilliseconds) > stalenessThresholdMilliseconds
        } catch (_: Exception) {
            true
        }
    }
}