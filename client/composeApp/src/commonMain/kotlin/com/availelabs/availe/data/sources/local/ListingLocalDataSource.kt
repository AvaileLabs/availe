package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.availelabs.availe.db.ClientDatabase
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingId
import kotlinx.coroutines.Deferred

interface IListingLocalDataSource {
    suspend fun upsertListing(listing: Listing)
    suspend fun getListingsPage(limit: Long, offset: Long): List<Listing>
    suspend fun getListingById(id: ListingId): Listing?
    suspend fun deleteListingById(id: ListingId)
    suspend fun deleteAllListings()
    suspend fun isListingDatabaseEmpty(): Boolean
}

class ListingLocalDataSource(
    private val databaseDeferred: Deferred<ClientDatabase>
) : IListingLocalDataSource {

    override suspend fun upsertListing(listing: Listing) {
        val database = databaseDeferred.await()
        database.transaction {
            database.listingsQueries.upsertListing(
                primary_key_id = listing.id,
                created_at = listing.createdAt,
                publisher_id = listing.commonData.publisherId,
                listing_type = when (listing) {
                    is Listing.Item -> ListingCategory.Item
                    is Listing.Event -> ListingCategory.Event
                    is Listing.Service -> ListingCategory.Service
                },
                title = listing.commonData.title,
                description = listing.commonData.description,
                price_amount = listing.commonData.price.amount,
                price_currency = listing.commonData.price.currency,
                image_links = listing.commonData.imageLinks
            )

            when (listing) {
                is Listing.Item -> database.listingsQueries.upsertItemDetails(listing.id)
                is Listing.Event -> database.listingsQueries.upsertEventDetails(listing.id)
                is Listing.Service -> database.listingsQueries.upsertServiceDetails(listing.id)
            }
        }
    }

    override suspend fun getListingsPage(limit: Long, offset: Long): List<Listing> {
        val database = databaseDeferred.await()
        return database.listingsQueries.selectPage(limit, offset)
            .awaitAsList()
            .map { selectPage -> selectPage.toDomain() }
    }

    override suspend fun getListingById(id: ListingId): Listing? {
        val database = databaseDeferred.await()
        return database.listingsQueries.selectById(id)
            .awaitAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun deleteListingById(id: ListingId) {
        val database = databaseDeferred.await()
        database.listingsQueries.deleteById(id)
    }

    override suspend fun deleteAllListings() {
        val database = databaseDeferred.await()
        database.listingsQueries.deleteAll()
    }

    override suspend fun isListingDatabaseEmpty(): Boolean {
        val database = databaseDeferred.await()
        val count = database.listingsQueries.countListings().awaitAsOne()
        return count == 0L
    }
}