package com.availelabs.availe.sources.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.availelabs.availe.database.IDatabaseProvider
import com.availelabs.availe.database.toDomain
import com.availelabs.availe.domain.model.*

interface IListingLocalDataSource {
    suspend fun upsertListing(listing: Listing)
    suspend fun getListings(
        limit: Long,
        offset: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing>

    suspend fun getListingById(id: ListingId): Listing?
    suspend fun deleteListingById(id: ListingId)
    suspend fun deleteListings(publisherId: UserId?, filter: ListingCategoryFilter)
    suspend fun isListingDatabaseEmpty(): Boolean
}

internal class ListingLocalDataSource(
    private val databaseProvider: IDatabaseProvider
) : IListingLocalDataSource {

    override suspend fun upsertListing(listing: Listing) {
        val database = databaseProvider.getDatabase()
        database.transaction {
            database.listingsQueries.upsertListing(
                primary_key_id = listing.id,
                created_at = listing.createdAt,
                publisher_id = listing.commonData.publisherId,
                publisher_email = listing.commonData.publisherEmail,
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

    override suspend fun getListings(
        limit: Long,
        offset: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing> {
        val database = databaseProvider.getDatabase()
        val categories = if (filter.isAll) {
            ListingCategory.entries
        } else {
            filter.categories.toList()
        }

        return database.listingsQueries.selectListings(
            publisher_id = publisherId,
            categories = categories,
            limit = limit,
            offset = offset
        )
            .awaitAsList()
            .map { selectListings -> selectListings.toDomain() }
    }

    override suspend fun getListingById(id: ListingId): Listing? {
        val database = databaseProvider.getDatabase()
        return database.listingsQueries.selectById(id)
            .awaitAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun deleteListingById(id: ListingId) {
        val database = databaseProvider.getDatabase()
        database.listingsQueries.deleteById(id)
    }

    override suspend fun deleteListings(publisherId: UserId?, filter: ListingCategoryFilter) {
        val database = databaseProvider.getDatabase()
        val categories = if (filter.isAll) {
            ListingCategory.entries
        } else {
            filter.categories.toList()
        }

        database.listingsQueries.deleteListings(
            publisher_id = publisherId,
            categories = categories
        )
    }

    override suspend fun isListingDatabaseEmpty(): Boolean {
        val database = databaseProvider.getDatabase()
        val count = database.listingsQueries.countListings().awaitAsOne()
        return count == 0L
    }
}