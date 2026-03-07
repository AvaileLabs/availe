package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.extensions.toAppError
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.model.ListingDraft
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.network.toAppError
import com.availelabs.availe.preferences.IListingPreferences
import com.availelabs.availe.preferences.ListingFetchConfiguration
import com.availelabs.availe.repositories.model.ListingStoreKey
import com.availelabs.availe.repositories.store.ListingStoreFactory
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
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
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    override fun getListingsPageStream(
        page: Int,
        size: Int,
        forceRefresh: Boolean,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ): Flow<DataStream<List<Listing>>> = flow {
        emit(DataStream.Loading)

        val shouldRefresh = if (forceRefresh) true else isListingCacheStale(filter, eventFeedFilter)
        val listingStoreKey = ListingStoreKey(page, size, publisherId, filter, eventFeedFilter)
        val fetchConfiguration = ListingFetchConfiguration(filter, eventFeedFilter)

        val storeReadRequest = if (shouldRefresh) {
            StoreReadRequest.cached(listingStoreKey, refresh = true)
        } else {
            StoreReadRequest.cached(listingStoreKey, refresh = false)
        }

        var lastLoadedData: List<Listing>? = null

        val dataStream = store.stream(storeReadRequest)
            .filterNot { storeReadResponse ->
                shouldRefresh &&
                        storeReadResponse is StoreReadResponse.Data &&
                        storeReadResponse.origin is StoreReadResponseOrigin.SourceOfTruth &&
                        storeReadResponse.value.isEmpty()
            }
            .transform { storeReadResponse ->
                if (storeReadResponse.origin is StoreReadResponseOrigin.Fetcher && storeReadResponse is StoreReadResponse.Data) {
                    try {
                        listingPreferences.updateLastListingsFetchTime(
                            timestampMilliseconds = Clock.System.now().toEpochMilliseconds(),
                            configuration = fetchConfiguration
                        )
                    } catch (_: Exception) {
                    }
                }

                when (storeReadResponse) {
                    is StoreReadResponse.Data -> {
                        val responseData = storeReadResponse.requireData()
                        lastLoadedData = responseData
                        emit(DataStream.Data(responseData))
                    }

                    is StoreReadResponse.Error -> {
                        emit(DataStream.Error(storeReadResponse.toAppError()))
                    }

                    is StoreReadResponse.Loading -> {
                        emit(DataStream.Loading)
                    }

                    is StoreReadResponse.NoNewData -> {
                        if (lastLoadedData != null) {
                            emit(DataStream.Data(lastLoadedData!!))
                        }
                    }

                    is StoreReadResponse.Initial -> {
                    }
                }
            }
            .catch { exception ->
                emit(DataStream.Error(exception.toAppError()))
            }

        emitAll(dataStream)
    }

    override fun getListingByIdStream(id: ListingId): Flow<Listing?> {
        return localDataSource.getListingByIdStream(id)
    }

    override suspend fun prefetchListings(
        page: Int,
        size: Int,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ) {
        val listingStoreKey = ListingStoreKey(page, size, publisherId, filter, eventFeedFilter)
        val fetchConfiguration = ListingFetchConfiguration(filter, eventFeedFilter)
        try {
            store.fresh(listingStoreKey)
            listingPreferences.updateLastListingsFetchTime(
                timestampMilliseconds = Clock.System.now().toEpochMilliseconds(),
                configuration = fetchConfiguration
            )
        } catch (_: Exception) {
        }
    }

    override suspend fun fetchAndCacheListingById(id: ListingId): RepositoryOperationResult<Unit> {
        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.getListingById(id)
        }

        return when (networkResult) {
            is RepositoryOperationResult.Success -> {
                when (val findResult = networkResult.value) {
                    is FindResult.Found -> {
                        try {
                            localDataSource.upsertListing(findResult.value)
                            RepositoryOperationResult.Success(Unit)
                        } catch (exception: Exception) {
                            RepositoryOperationResult.Failure(AppError.UnknownError(exception.message))
                        }
                    }

                    is FindResult.NotFound -> {
                        RepositoryOperationResult.Failure(AppError.BusinessError.ResourceNotFound)
                    }
                }
            }

            is RepositoryOperationResult.Failure -> {
                RepositoryOperationResult.Failure(networkResult.error)
            }
        }
    }

    override suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>> {
        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.createListing(newListing)
        }

        if (networkResult is RepositoryOperationResult.Success) {
            val insertResult = networkResult.value
            if (insertResult is InsertResult.Inserted) {
                try {
                    localDataSource.upsertListing(insertResult.value)
                    store.clear()
                    _updates.emit(Unit)
                    clearDraft()
                } catch (_: Exception) {
                }
            }
        }

        return networkResult
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
        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.updateListing(id, patch)
        }

        if (networkResult is RepositoryOperationResult.Success) {
            val updateResult = networkResult.value
            if (updateResult is UpdateResult.Updated) {
                try {
                    localDataSource.upsertListing(updateResult.value)
                    store.clear()
                    _updates.emit(Unit)
                    clearDraft()
                } catch (_: Exception) {
                }
            }
        }

        return networkResult
    }

    override suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult> {
        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.deleteListing(id)
        }

        if (networkResult is RepositoryOperationResult.Success && networkResult.value is DeleteResult.Deleted) {
            try {
                localDataSource.deleteListingById(id)
                store.clear()
                _updates.emit(Unit)
            } catch (_: Exception) {
            }
        }

        return networkResult
    }

    override suspend fun isListingCacheStale(filter: ListingCategoryFilter, eventFeedFilter: EventFeedFilter): Boolean {
        return try {
            if (localDataSource.isListingDatabaseEmpty()) {
                return true
            }

            val fetchConfiguration = ListingFetchConfiguration(filter, eventFeedFilter)
            val lastFetchTimeMilliseconds =
                listingPreferences.getLastListingsFetchTimeMilliseconds(fetchConfiguration) ?: return true

            val currentTimestampMilliseconds = Clock.System.now().toEpochMilliseconds()
            val stalenessThresholdMilliseconds = 15 * 60 * 1000

            (currentTimestampMilliseconds - lastFetchTimeMilliseconds) > stalenessThresholdMilliseconds
        } catch (_: Exception) {
            true
        }
    }

    override suspend fun saveDraft(draft: ListingDraft) {
        try {
            val draftJsonString = jsonSerializer.encodeToString(draft)
            listingPreferences.saveDraft(draftJsonString)
        } catch (_: Exception) {
        }
    }

    override suspend fun getDraft(): ListingDraft? {
        return try {
            val draftJsonString = listingPreferences.getDraft() ?: return null
            jsonSerializer.decodeFromString<ListingDraft>(draftJsonString)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearDraft() {
        try {
            listingPreferences.clearDraft()
        } catch (_: Exception) {
        }
    }
}