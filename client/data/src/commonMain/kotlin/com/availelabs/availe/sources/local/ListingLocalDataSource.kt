package com.availelabs.availe.sources.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.availelabs.availe.database.IDatabaseProvider
import com.availelabs.availe.database.toDomain
import com.availelabs.availe.domain.model.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

interface IListingLocalDataSource {
    suspend fun upsertListing(listing: Listing)
    suspend fun updateRsvpStatus(id: ListingId, isRsvped: Boolean)
    suspend fun updateFavoriteStatus(id: ListingId, isFavorited: Boolean)
    fun getListings(
        limit: Long,
        offset: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    ): Flow<List<Listing>>

    suspend fun getListingById(id: ListingId): Listing?
    suspend fun deleteListingById(id: ListingId)
    suspend fun deleteListings(
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    )

    suspend fun isListingDatabaseEmpty(): Boolean
}

internal class ListingLocalDataSource(
    private val databaseProvider: IDatabaseProvider
) : IListingLocalDataSource {

    override suspend fun upsertListing(listing: Listing) {
        val database = databaseProvider.getDatabase()
        database.transaction {
            database.listingQueries.upsertListing(
                primary_key_id = listing.id,
                created_at = listing.createdAt,
                publisher_id = listing.publisher.id,
                publisher_email = Email(listing.publisher.jid.value),
                listing_type = when (listing) {
                    is Listing.Item -> ListingCategory.Item
                    is Listing.Event -> ListingCategory.Event
                    is Listing.Service -> ListingCategory.Service
                },
                title = listing.commonData.title,
                description = listing.commonData.description,
                price_amount = listing.commonData.price.amount,
                price_currency = listing.commonData.price.currency,
                image_links = listing.commonData.imageLinks,
                is_rsvped = listing.interactionState.isRsvped,
                is_favorited = listing.interactionState.isFavorited
            )

            when (listing) {
                is Listing.Item -> database.listingQueries.upsertItemDetails(listing.id)
                is Listing.Event -> database.listingQueries.upsertEventDetails(listing.id, listing.startTime)
                is Listing.Service -> database.listingQueries.upsertServiceDetails(listing.id)
            }
        }
    }

    override suspend fun updateRsvpStatus(id: ListingId, isRsvped: Boolean) {
        val database = databaseProvider.getDatabase()
        database.listingQueries.updateRsvpStatus(is_rsvped = isRsvped, primary_key_id = id)
    }

    override suspend fun updateFavoriteStatus(id: ListingId, isFavorited: Boolean) {
        val database = databaseProvider.getDatabase()
        database.listingQueries.updateFavoriteStatus(is_favorited = isFavorited, primary_key_id = id)
    }

    override fun getListings(
        limit: Long,
        offset: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ): Flow<List<Listing>> = flow {
        val database = databaseProvider.getDatabase()
        val categories = if (filter.isAll) {
            ListingCategory.entries
        } else {
            filter.categories.toList()
        }

        val queryFlow = database.listingQueries.selectListings(
            publisher_id = publisherId,
            categories = categories,
            limit = limit,
            offset = offset
        ).asFlow().map { query -> query.awaitAsList() }

        emitAll(
            queryFlow.map { selectListings ->
                val currentInstant = Clock.System.now()
                selectListings.map { it.toDomain() }.filter { listing ->
                    if (listing !is Listing.Event) return@filter true
                    when (eventFeedFilter) {
                        EventFeedFilter.None -> true
                        EventFeedFilter.Upcoming -> listing.startTime >= currentInstant
                        EventFeedFilter.Past -> listing.startTime < currentInstant
                        EventFeedFilter.Going -> listing.interactionState.isRsvped
                    }
                }
            }
        )
    }

    override suspend fun getListingById(id: ListingId): Listing? {
        val database = databaseProvider.getDatabase()
        return database.listingQueries.selectById(id)
            .awaitAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun deleteListingById(id: ListingId) {
        val database = databaseProvider.getDatabase()
        database.listingQueries.deleteById(id)
    }

    override suspend fun deleteListings(
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ) {
        val database = databaseProvider.getDatabase()
        val categories = if (filter.isAll) {
            ListingCategory.entries
        } else {
            filter.categories.toList()
        }

        if (eventFeedFilter == EventFeedFilter.None) {
            database.listingQueries.deleteListings(
                publisher_id = publisherId,
                categories = categories
            )
        } else {
            val listingsToDelete = getListings(
                limit = Long.MAX_VALUE,
                offset = 0L,
                publisherId = publisherId,
                filter = filter,
                eventFeedFilter = eventFeedFilter
            ).first()

            database.transaction {
                listingsToDelete.forEach { listing ->
                    database.listingQueries.deleteById(listing.id)
                }
            }
        }
    }

    override suspend fun isListingDatabaseEmpty(): Boolean {
        val database = databaseProvider.getDatabase()
        val count = database.listingQueries.countListings().awaitAsOne()
        return count == 0L
    }
}