package com.availelabs.availe.repositories.store

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.repositories.model.ListingStoreKey
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.local.ILocalDataCleaner
import com.availelabs.availe.sources.remote.IListingRemoteDataSource
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

internal class ListingStoreFactory(
    private val remoteDataSource: IListingRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    private val localDataCleaner: ILocalDataCleaner
) {
    fun create(): Store<ListingStoreKey, List<Listing>> {
        return StoreBuilder.Companion.from<ListingStoreKey, List<Listing>, List<Listing>>(
            fetcher = Fetcher.Companion.of { key ->
                remoteDataSource.getListingsPage(
                    page = key.page,
                    size = key.size,
                    publisherId = key.publisherId,
                    filter = key.filter
                )
            },
            sourceOfTruth = SourceOfTruth.Companion.of(
                nonFlowReader = { key ->
                    try {
                        val offset = (key.page * key.size).toLong()
                        val limit = key.size.toLong()

                        localDataSource.getListings(
                            limit = limit,
                            offset = offset,
                            publisherId = key.publisherId,
                            filter = key.filter
                        )
                    } catch (_: Exception) {
                        localDataCleaner.clearAllData()
                        null
                    }
                },
                writer = { key, listings ->
                    try {
                        if (key.page == 0) {
                            localDataSource.deleteListings(key.publisherId, key.filter)
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