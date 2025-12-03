package com.availelabs.availe.data.repositories

import com.availelabs.availe.data.sources.local.IListingLocalDataSource
import com.availelabs.availe.data.sources.remote.IListingRemoteDataSource
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingStoreKey
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class ListingStoreFactory(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource
) {
    fun create(): Store<ListingStoreKey, List<Listing>> {
        return StoreBuilder.from<ListingStoreKey, List<Listing>, List<Listing>>(
            fetcher = Fetcher.of { key ->
                remoteDataSource.getListingsPage(key.page, key.size)
            },
            sourceOfTruth = SourceOfTruth.of(
                nonFlowReader = { key ->
                    val offset = (key.page * key.size).toLong()
                    val limit = key.size.toLong()
                    localDataSource.getListingsPage(limit, offset)
                },
                writer = { key, listings ->
                    if (key.page == 0) {
                        localDataSource.deleteAllListings()
                    }
                    listings.forEach { listing ->
                        localDataSource.upsertListing(listing)
                    }
                }
            )
        ).build()
    }
}