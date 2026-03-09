package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.model.ListingDraft
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.preferences.IListingPreferences
import com.availelabs.availe.preferences.ListingFetchConfiguration
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.types.PageSyncResult
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class ListingRepository(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    private val listingPreferences: IListingPreferences,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IListingRepository {

    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    override fun observeFeed(
        limit: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ): Flow<List<Listing>> {
        return localDataSource.getListings(
            limit = limit,
            publisherId = publisherId,
            filter = filter,
            eventFeedFilter = eventFeedFilter
        )
    }

    override suspend fun syncFeedPage(
        page: Int,
        size: Int,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ): PageSyncResult {
        val fetchConfiguration = ListingFetchConfiguration(filter, eventFeedFilter)

        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.getListingsPage(
                page = page,
                size = size,
                publisherId = publisherId,
                filter = filter,
                eventFeedFilter = eventFeedFilter
            )
        }

        return when (networkResult) {
            is RepositoryOperationResult.Success -> {
                try {
                    val fetchedListings = networkResult.value
                    if (page == 0) {
                        localDataSource.deleteListings(
                            publisherId = publisherId,
                            filter = filter,
                            eventFeedFilter = eventFeedFilter
                        )
                    }
                    fetchedListings.forEach { listing ->
                        localDataSource.upsertListing(listing)
                    }
                    listingPreferences.updateLastListingsFetchTime(
                        timestampMilliseconds = Clock.System.now().toEpochMilliseconds(),
                        configuration = fetchConfiguration
                    )

                    val itemsFetchedCount = fetchedListings.size
                    val hasMoreAvailable = itemsFetchedCount >= size

                    PageSyncResult.Success(
                        itemsFetched = itemsFetchedCount,
                        hasMoreAvailable = hasMoreAvailable
                    )
                } catch (exception: Exception) {
                    PageSyncResult.Failure(AppError.UnknownError(exception.message))
                }
            }

            is RepositoryOperationResult.Failure -> {
                PageSyncResult.Failure(networkResult.error)
            }
        }
    }

    override fun getListingByIdStream(id: ListingId): Flow<Listing?> {
        return localDataSource.getListingByIdStream(id)
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