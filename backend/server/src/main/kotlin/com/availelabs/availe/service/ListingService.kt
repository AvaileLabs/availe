@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.service

import com.availelabs.availe.domain.extensions.canBeModifiedBy
import com.availelabs.availe.domain.extensions.withInteractionState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.interaction.IInteractionRepository
import com.availelabs.availe.repositories.listing.IListingRepository
import kotlin.uuid.ExperimentalUuidApi

interface IListingService {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing>
    fun getAllListings(viewingUserId: UserId?): List<Listing>
    fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    ): List<Listing>

    fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing>

    fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult
}

class ListingService(
    private val listingRepository: IListingRepository,
    private val interactionRepository: IInteractionRepository
) : IListingService {

    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        return listingRepository.createListing(newListing)
    }

    override fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing> {
        val findResult = listingRepository.getListingById(id)
        if (findResult is FindResult.NotFound) {
            return FindResult.NotFound
        }

        val foundListing = (findResult as FindResult.Found).value

        if (viewingUserId == null) {
            return FindResult.Found(foundListing)
        }

        val listingIdentifiersList = listOf(foundListing.id)
        val interactionStateMap = interactionRepository.getUserInteractions(viewingUserId, listingIdentifiersList)
        val listingInteractionState =
            interactionStateMap[foundListing.id] ?: InteractionState(isFavorited = false, isRsvped = false)

        val updatedListing = foundListing.withInteractionState(listingInteractionState)
        return FindResult.Found(updatedListing)
    }

    override fun getAllListings(viewingUserId: UserId?): List<Listing> {
        val databaseListings = listingRepository.getAllListings()
        if (viewingUserId == null || databaseListings.isEmpty()) {
            return databaseListings
        }

        val listingIdentifiersList = databaseListings.map { it.id }
        val interactionStateMap = interactionRepository.getUserInteractions(viewingUserId, listingIdentifiersList)

        return databaseListings.map { listing ->
            val listingInteractionState =
                interactionStateMap[listing.id] ?: InteractionState(isFavorited = false, isRsvped = false)
            listing.withInteractionState(listingInteractionState)
        }
    }

    override fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing> {
        val databaseListings = listingRepository.getListingsPage(pageNumber, pageSize, publisherId, filter)
        if (viewingUserId == null || databaseListings.isEmpty()) {
            return databaseListings
        }

        val listingIdentifiersList = databaseListings.map { it.id }
        val interactionStateMap = interactionRepository.getUserInteractions(viewingUserId, listingIdentifiersList)

        return databaseListings.map { listing ->
            val listingInteractionState =
                interactionStateMap[listing.id] ?: InteractionState(isFavorited = false, isRsvped = false)
            listing.withInteractionState(listingInteractionState)
        }
    }

    override fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing> {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return UpdateResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return UpdateResult.Forbidden
        }

        return listingRepository.updateListing(listingId, patch)
    }

    override fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult {
        val listing = when (val result = listingRepository.getListingById(listingId)) {
            is FindResult.Found -> result.value
            is FindResult.NotFound -> return DeleteResult.NotFound
        }

        if (!listing.canBeModifiedBy(requestingPublisherId)) {
            return DeleteResult.Forbidden
        }

        return listingRepository.deleteListing(listingId)
    }
}