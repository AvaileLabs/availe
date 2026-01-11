package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.extensions.toAppError
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.network.toAppError
import com.availelabs.availe.preferences.IListingPreferences
import com.availelabs.availe.repositories.model.ListingStoreKey
import com.availelabs.availe.repositories.store.ListingStoreFactory
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.*
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalStoreApi::class)
internal class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    storeFactory: ListingStoreFactory,
    private val listingPreferences: IListingPreferences,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IListingRepository {

    private val store = storeFactory.create()
    private val _updates = MutableSharedFlow<Unit>()
    override val updates: Flow<Unit> = _updates.asSharedFlow()

    override fun getListingsPageStream(
        page: Int,
        size: Int,
        forceRefresh: Boolean,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): Flow<DataStream<List<Listing>>> = flow {
        emit(DataStream.Loading)

        val shouldRefresh = if (forceRefresh) true else isListingCacheStale(filter)
        val key = ListingStoreKey(page, size, publisherId, filter)

        val request = if (shouldRefresh) {
            StoreReadRequest.cached(key, refresh = true)
        } else {
            StoreReadRequest.cached(key, refresh = false)
        }

        var lastData: List<Listing>? = null

        val stream = store.stream(request)
            .transform { response ->
                if (response.origin is StoreReadResponseOrigin.Fetcher && response is StoreReadResponse.Data) {
                    try {
                        listingPreferences.updateLastListingsFetchTime(
                            timestampMilliseconds = Clock.System.now().toEpochMilliseconds(),
                            filter = key.filter
                        )
                    } catch (_: Exception) {
                    }
                }

                when (response) {
                    is StoreReadResponse.Data -> {
                        val data = response.requireData()
                        lastData = data
                        emit(DataStream.Data(data))
                    }

                    is StoreReadResponse.Error -> {
                        emit(DataStream.Error(response.toAppError()))
                    }

                    is StoreReadResponse.Loading -> {
                        emit(DataStream.Loading)
                    }

                    is StoreReadResponse.NoNewData -> {
                        if (lastData != null) {
                            emit(DataStream.Data(lastData))
                        }
                    }

                    is StoreReadResponse.Initial -> {
                    }
                }
            }
            .catch { exception ->
                emit(DataStream.Error(exception.toAppError()))
            }

        emitAll(stream)
    }

    override suspend fun prefetchListings(
        page: Int,
        size: Int,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ) {
        val key = ListingStoreKey(page, size, publisherId, filter)
        try {
            store.fresh(key)
            listingPreferences.updateLastListingsFetchTime(
                timestampMilliseconds = Clock.System.now().toEpochMilliseconds(),
                filter = filter
            )
        } catch (_: Exception) {
        }
    }

    override suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>> {
        return safeNetworkCaller.execute {
            remoteDataSource.getListingById(id)
        }
    }

    override suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>> {
        val result = safeNetworkCaller.execute {
            remoteDataSource.createListing(newListing)
        }

        if (result is RepositoryOperationResult.Success) {
            val insertResult = result.value
            if (insertResult is InsertResult.Inserted) {
                try {
                    localDataSource.upsertListing(insertResult.value)
                    store.clear()
                    _updates.emit(Unit)
                } catch (_: Exception) {
                }
            }
        }

        return result
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
        val result = safeNetworkCaller.execute {
            remoteDataSource.updateListing(id, patch)
        }

        if (result is RepositoryOperationResult.Success) {
            val updateResult = result.value
            if (updateResult is UpdateResult.Updated) {
                try {
                    localDataSource.upsertListing(updateResult.value)
                    store.clear()
                    _updates.emit(Unit)
                } catch (_: Exception) {
                }
            }
        }

        return result
    }

    override suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult> {
        val result = safeNetworkCaller.execute {
            remoteDataSource.deleteListing(id)
        }

        if (result is RepositoryOperationResult.Success && result.value is DeleteResult.Deleted) {
            try {
                localDataSource.deleteListingById(id)
                store.clear()
                _updates.emit(Unit)
            } catch (_: Exception) {
            }
        }

        return result
    }

    override suspend fun isListingCacheStale(filter: ListingCategoryFilter): Boolean {
        return try {
            if (localDataSource.isListingDatabaseEmpty()) {
                return true
            }

            val lastFetchTimeMilliseconds =
                listingPreferences.getLastListingsFetchTimeMilliseconds(filter) ?: return true

            val currentTimestampMilliseconds = Clock.System.now().toEpochMilliseconds()
            val stalenessThresholdMilliseconds = 15 * 60 * 1000

            (currentTimestampMilliseconds - lastFetchTimeMilliseconds) > stalenessThresholdMilliseconds
        } catch (_: Exception) {
            true
        }
    }
}