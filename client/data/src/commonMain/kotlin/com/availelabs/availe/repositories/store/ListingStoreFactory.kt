package com.availelabs.availe.repositories.store

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.repositories.model.ListingStoreKey
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

internal class ListingStoreFactory(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource
) {
    fun create(): Store<ListingStoreKey, List<Listing>> {
        return StoreBuilder.from<ListingStoreKey, List<Listing>, List<Listing>>(
            fetcher = Fetcher.Companion.of { listingStoreKey ->
                remoteDataSource.getListingsPage(
                    page = listingStoreKey.page,
                    size = listingStoreKey.size,
                    publisherId = listingStoreKey.publisherId,
                    filter = listingStoreKey.filter,
                    eventFeedFilter = listingStoreKey.eventFeedFilter
                )
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                reader = { listingStoreKey ->
                    val queryOffset = (listingStoreKey.page * listingStoreKey.size).toLong()
                    val queryLimit = listingStoreKey.size.toLong()

                    localDataSource.getListings(
                        limit = queryLimit,
                        offset = queryOffset,
                        publisherId = listingStoreKey.publisherId,
                        filter = listingStoreKey.filter,
                        eventFeedFilter = listingStoreKey.eventFeedFilter
                    )
                },
                writer = { listingStoreKey, listingsList ->
                    if (listingStoreKey.page == 0) {
                        localDataSource.deleteListings(
                            publisherId = listingStoreKey.publisherId,
                            filter = listingStoreKey.filter,
                            eventFeedFilter = listingStoreKey.eventFeedFilter
                        )
                    }
                    listingsList.forEach { listing ->
                        localDataSource.upsertListing(listing)
                    }
                }
            )
        ).build()
    }
}