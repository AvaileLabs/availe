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
            fetcher = Fetcher.Companion.of { key ->
                remoteDataSource.getListingsPage(
                    page = key.page,
                    size = key.size,
                    publisherId = key.publisherId,
                    filter = key.filter,
                    eventFeedFilter = key.eventFeedFilter
                )
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                nonFlowReader = { key ->
                    val offset = (key.page * key.size).toLong()
                    val limit = key.size.toLong()

                    localDataSource.getListings(
                        limit = limit,
                        offset = offset,
                        publisherId = key.publisherId,
                        filter = key.filter,
                        eventFeedFilter = key.eventFeedFilter
                    )
                },
                writer = { key, listings ->
                    if (key.page == 0) {
                        localDataSource.deleteListings(key.publisherId, key.filter, key.eventFeedFilter)
                    }
                    listings.forEach { listing ->
                        localDataSource.upsertListing(listing)
                    }
                }
            )
        ).build()
    }
}