package com.availelabs.availe.sources.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.availelabs.availe.database.IDatabaseProvider
import com.availelabs.availe.database.toDomain
import com.availelabs.availe.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

interface IListingLocalDataSource {
    suspend fun upsertListing(listing: Listing)
    fun getListings(
        limit: Long,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    ): Flow<List<Listing>>

    fun getListingByIdStream(id: ListingId): Flow<Listing?>

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
                is Listing.Event -> database.listingQueries.upsertEventDetails(
                    listing_id = listing.id,
                    start_local_time = listing.schedule.startLocalTime,
                    end_local_time = listing.schedule.endLocalTime,
                    start_instant = listing.schedule.startInstant,
                    end_instant = listing.schedule.endInstant,
                    time_zone_identifier = listing.schedule.timeZoneIdentifier
                )

                is Listing.Service -> database.listingQueries.upsertServiceDetails(listing.id)
            }
        }
    }

    override fun getListings(
        limit: Long,
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

        when (eventFeedFilter) {
            EventFeedFilter.None -> {
                val queryFlow = database.listingQueries.observeListings(
                    publisher_id = publisherId,
                    categories = categories,
                    limit = limit
                ).asFlow().map { query ->
                    query.awaitAsList().map { observeListings -> observeListings.toDomain() }
                }
                emitAll(queryFlow)
            }

            EventFeedFilter.Upcoming -> {
                val queryFlow = database.listingQueries.observeUpcomingEvents(
                    publisher_id = publisherId,
                    current_instant = Clock.System.now(),
                    limit = limit
                ).asFlow().map { query ->
                    query.awaitAsList().map { observeUpcomingEvents -> observeUpcomingEvents.toDomain() }
                }
                emitAll(queryFlow)
            }

            EventFeedFilter.Past -> {
                val queryFlow = database.listingQueries.observePastEvents(
                    publisher_id = publisherId,
                    current_instant = Clock.System.now(),
                    limit = limit
                ).asFlow().map { query ->
                    query.awaitAsList().map { observePastEvents -> observePastEvents.toDomain() }
                }
                emitAll(queryFlow)
            }

            EventFeedFilter.Going -> {
                val queryFlow = database.listingQueries.observeGoingEvents(
                    publisher_id = publisherId,
                    limit = limit
                ).asFlow().map { query ->
                    query.awaitAsList().map { observeGoingEvents -> observeGoingEvents.toDomain() }
                }
                emitAll(queryFlow)
            }
        }
    }

    override fun getListingByIdStream(id: ListingId): Flow<Listing?> = flow {
        val database = databaseProvider.getDatabase()
        val queryFlow = database.listingQueries.selectById(id).asFlow().map { query -> query.awaitAsOneOrNull() }

        emitAll(
            queryFlow.map { selectByIdResult ->
                selectByIdResult?.toDomain()
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

        when (eventFeedFilter) {
            EventFeedFilter.None -> {
                database.listingQueries.deleteListings(
                    publisher_id = publisherId,
                    categories = categories
                )
            }

            EventFeedFilter.Upcoming -> {
                database.listingQueries.deleteUpcomingEvents(
                    publisher_id = publisherId,
                    current_instant = Clock.System.now()
                )
            }

            EventFeedFilter.Past -> {
                database.listingQueries.deletePastEvents(
                    publisher_id = publisherId,
                    current_instant = Clock.System.now()
                )
            }

            EventFeedFilter.Going -> {
                database.listingQueries.deleteGoingEvents(
                    publisher_id = publisherId
                )
            }
        }
    }

    override suspend fun isListingDatabaseEmpty(): Boolean {
        val database = databaseProvider.getDatabase()
        val count = database.listingQueries.countListings().awaitAsOne()
        return count == 0L
    }
}