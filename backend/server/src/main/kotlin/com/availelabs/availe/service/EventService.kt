package com.availelabs.availe.service

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.repositories.event.IEventRepository
import com.availelabs.availe.repositories.interaction.IInteractionRepository

interface IEventService {
    fun getEventFeedPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId?,
        eventFeedFilter: EventFeedFilter
    ): List<Listing.Event>
}

class EventService(
    private val eventRepository: IEventRepository,
    private val interactionRepository: IInteractionRepository
) : IEventService {

    override fun getEventFeedPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId?,
        eventFeedFilter: EventFeedFilter
    ): List<Listing.Event> {
        var rsvpListingIdentifiersList: List<ListingId>? = null

        if (eventFeedFilter == EventFeedFilter.Going) {
            if (viewingUserId == null) {
                return emptyList()
            }
            rsvpListingIdentifiersList = interactionRepository.getListingIdentifiersWithRsvp(viewingUserId)
            if (rsvpListingIdentifiersList.isEmpty()) {
                return emptyList()
            }
        }

        val databaseEventListings = eventRepository.getEventFeedPage(
            pageNumber = pageNumber,
            pageSize = pageSize,
            publisherIdentifier = publisherId,
            eventFeedFilter = eventFeedFilter,
            rsvpListingIdentifiers = rsvpListingIdentifiersList
        )

        if (viewingUserId == null || databaseEventListings.isEmpty()) {
            return databaseEventListings
        }

        val listingIdentifiersList = databaseEventListings.map { it.id }
        val interactionStateMap = interactionRepository.getUserInteractions(viewingUserId, listingIdentifiersList)

        return databaseEventListings.map { eventListing ->
            val listingInteractionState =
                interactionStateMap[eventListing.id] ?: InteractionState(isFavorited = false, isRsvped = false)
            eventListing.withInteractionState(listingInteractionState) as Listing.Event
        }
    }
}