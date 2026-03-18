package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingCategoryFilter
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.model.EventFeedQuery
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.preferences.IListingPreferences
import com.availelabs.availe.preferences.ListingFetchConfiguration
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IEventRemoteDataSource
import com.availelabs.availe.types.PageSyncResult
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
internal class EventRepository(
    private val remoteDataSource: IEventRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    private val listingPreferences: IListingPreferences,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IEventRepository {

    override fun observeEventFeed(
        limit: Long,
        publisherId: UserId?,
        query: EventFeedQuery
    ): Flow<List<Listing.Event>> {
        return when (query) {
            is EventFeedQuery.Upcoming -> {
                query.tickingNow.flatMapLatest {
                    localDataSource.getListings(
                        limit = limit,
                        publisherId = publisherId,
                        filter = ListingCategoryFilter.All,
                        eventFeedFilter = EventFeedFilter.Upcoming
                    ).map { listingsList ->
                        listingsList.filterIsInstance<Listing.Event>()
                    }
                }
            }

            is EventFeedQuery.Past -> {
                query.tickingNow.flatMapLatest {
                    localDataSource.getListings(
                        limit = limit,
                        publisherId = publisherId,
                        filter = ListingCategoryFilter.All,
                        eventFeedFilter = EventFeedFilter.Past
                    ).map { listingsList ->
                        listingsList.filterIsInstance<Listing.Event>()
                    }
                }
            }

            is EventFeedQuery.Going -> {
                localDataSource.getListings(
                    limit = limit,
                    publisherId = publisherId,
                    filter = ListingCategoryFilter.All,
                    eventFeedFilter = EventFeedFilter.Going
                ).map { listingsList ->
                    listingsList.filterIsInstance<Listing.Event>()
                }
            }
        }
    }

    override suspend fun syncEventFeedPage(
        page: Int,
        size: Int,
        publisherId: UserId?,
        query: EventFeedQuery
    ): PageSyncResult {
        val eventFeedFilter = when (query) {
            is EventFeedQuery.Upcoming -> EventFeedFilter.Upcoming
            is EventFeedQuery.Past -> EventFeedFilter.Past
            is EventFeedQuery.Going -> EventFeedFilter.Going
        }

        val fetchConfiguration = ListingFetchConfiguration(ListingCategoryFilter.All, eventFeedFilter)

        val networkResult = safeNetworkCaller.execute {
            remoteDataSource.getEventFeedPage(
                page = page,
                size = size,
                publisherId = publisherId,
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
                            filter = ListingCategoryFilter.All,
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

    override suspend fun isEventCacheStale(query: EventFeedQuery): Boolean {
        return try {
            if (localDataSource.isListingDatabaseEmpty()) {
                return true
            }

            val eventFeedFilter = when (query) {
                is EventFeedQuery.Upcoming -> EventFeedFilter.Upcoming
                is EventFeedQuery.Past -> EventFeedFilter.Past
                is EventFeedQuery.Going -> EventFeedFilter.Going
            }

            val fetchConfiguration = ListingFetchConfiguration(ListingCategoryFilter.All, eventFeedFilter)
            val lastFetchTimeMilliseconds =
                listingPreferences.getLastListingsFetchTimeMilliseconds(fetchConfiguration) ?: return true

            val currentTimestampMilliseconds = Clock.System.now().toEpochMilliseconds()
            val stalenessThresholdMilliseconds = 15 * 60 * 1000

            (currentTimestampMilliseconds - lastFetchTimeMilliseconds) > stalenessThresholdMilliseconds
        } catch (_: Exception) {
            true
        }
    }
}