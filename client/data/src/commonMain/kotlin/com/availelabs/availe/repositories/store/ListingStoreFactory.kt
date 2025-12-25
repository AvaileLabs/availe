package com.availelabs.availe.repositories.store

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.model.ListingStoreKey
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.local.ILocalDataCleaner
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class ListingStoreFactory(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    private val localDataCleaner: ILocalDataCleaner
) {
    fun create(): Store<ListingStoreKey, List<Listing>> {
        return StoreBuilder.Companion.from<ListingStoreKey, List<Listing>, List<Listing>>(
            fetcher = Fetcher.Companion.of { key ->
                remoteDataSource.getListingsPage(key.page, key.size)
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                nonFlowReader = { key ->
                    try {
                        val offset = (key.page * key.size).toLong()
                        val limit = key.size.toLong()
                        localDataSource.getListingsPage(limit, offset)
                    } catch (_: Exception) {
                        localDataCleaner.clearAllData()
                        null
                    }
                },
                writer = { key, listings ->
                    try {
                        if (key.page == 0) {
                            localDataSource.deleteAllListings()
                        }
                        listings.forEach { listing ->
                            localDataSource.upsertListing(listing)
                        }
                    } catch (_: Exception) {
                        localDataCleaner.clearAllData()
                    }
                }
            )
        ).build()
    }
}