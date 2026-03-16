@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.event

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.mappers.toDomainListing
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.*
import org.jooq.DSLContext
import org.jooq.impl.DSL
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

interface IEventRepository {
    fun getEventById(listingIdentifier: ListingId): FindResult<Listing.Event>
    fun getEventFeedPage(
        pageNumber: Int,
        pageSize: Int,
        publisherIdentifier: UserId?,
        eventFeedFilter: EventFeedFilter,
        rsvpListingIdentifiers: List<ListingId>?
    ): List<Listing.Event>
}

internal class EventRepository(
    private val databaseContext: DSLContext
) : IEventRepository {

    private fun baseEventQuery() = databaseContext
        .select(
            LISTINGS.asterisk(),
            USERS.ID,
            USERS.DISPLAY_NAME,
            USERS.AVATAR_URL,
            USERS.JID,
            LISTING_EVENT_DETAILS.START_LOCAL_TIME,
            LISTING_EVENT_DETAILS.END_LOCAL_TIME,
            LISTING_EVENT_DETAILS.START_INSTANT,
            LISTING_EVENT_DETAILS.END_INSTANT,
            LISTING_EVENT_DETAILS.TIME_ZONE_IDENTIFIER
        )
        .from(LISTINGS)
        .join(USERS).on(LISTINGS.PUBLISHER_ID.eq(USERS.ID))
        .join(LISTING_EVENT_DETAILS).on(LISTINGS.ID.eq(LISTING_EVENT_DETAILS.LISTING_ID))
        .leftJoin(LISTING_ITEM_DETAILS).on(DSL.falseCondition())
        .leftJoin(LISTING_SERVICE_DETAILS).on(DSL.falseCondition())

    override fun getEventById(listingIdentifier: ListingId): FindResult<Listing.Event> {
        val fetchedRecord = baseEventQuery()
            .where(LISTINGS.ID.eq(listingIdentifier.value.toJavaUuid()))
            .and(LISTINGS.LISTING_TYPE.eq(ListingTypeEnum.event))
            .fetchOne()

        if (fetchedRecord == null) {
            return FindResult.NotFound
        }

        val domainListing = fetchedRecord.toDomainListing(
            LISTINGS,
            USERS,
            LISTING_ITEM_DETAILS,
            LISTING_EVENT_DETAILS,
            LISTING_SERVICE_DETAILS
        )

        return FindResult.Found(domainListing as Listing.Event)
    }

    override fun getEventFeedPage(
        pageNumber: Int,
        pageSize: Int,
        publisherIdentifier: UserId?,
        eventFeedFilter: EventFeedFilter,
        rsvpListingIdentifiers: List<ListingId>?
    ): List<Listing.Event> {
        val offsetAmount = pageNumber * pageSize
        var databaseCondition = LISTINGS.LISTING_TYPE.eq(ListingTypeEnum.event)

        if (publisherIdentifier != null) {
            databaseCondition = databaseCondition.and(LISTINGS.PUBLISHER_ID.eq(publisherIdentifier.value.toJavaUuid()))
        }

        val currentOffsetDateTime = java.time.OffsetDateTime.now()
        var databaseOrderByField = LISTING_EVENT_DETAILS.START_INSTANT.asc()

        when (eventFeedFilter) {
            EventFeedFilter.Upcoming -> {
                databaseCondition = databaseCondition.and(LISTING_EVENT_DETAILS.END_INSTANT.ge(currentOffsetDateTime))
                databaseOrderByField = LISTING_EVENT_DETAILS.START_INSTANT.asc()
            }

            EventFeedFilter.Past -> {
                databaseCondition = databaseCondition.and(LISTING_EVENT_DETAILS.END_INSTANT.lt(currentOffsetDateTime))
                databaseOrderByField = LISTING_EVENT_DETAILS.END_INSTANT.desc()
            }

            EventFeedFilter.Going -> {
                if (rsvpListingIdentifiers != null && rsvpListingIdentifiers.isNotEmpty()) {
                    val javaUuidListingIdentifiers = rsvpListingIdentifiers.map { it.value.toJavaUuid() }
                    databaseCondition = databaseCondition.and(LISTINGS.ID.`in`(javaUuidListingIdentifiers))
                    databaseCondition =
                        databaseCondition.and(LISTING_EVENT_DETAILS.END_INSTANT.ge(currentOffsetDateTime))
                    databaseOrderByField = LISTING_EVENT_DETAILS.START_INSTANT.asc()
                } else {
                    databaseCondition = databaseCondition.and(DSL.falseCondition())
                }
            }

            EventFeedFilter.None -> {
                databaseOrderByField = LISTINGS.CREATED_AT.desc()
            }
        }

        return baseEventQuery()
            .where(databaseCondition)
            .orderBy(databaseOrderByField)
            .limit(pageSize)
            .offset(offsetAmount)
            .fetch()
            .map { databaseRecord ->
                val domainListing = databaseRecord.toDomainListing(
                    LISTINGS,
                    USERS,
                    LISTING_ITEM_DETAILS,
                    LISTING_EVENT_DETAILS,
                    LISTING_SERVICE_DETAILS
                )
                domainListing as Listing.Event
            }
    }
}