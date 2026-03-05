package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.types.RepositoryOperationResult

interface IInteractionRepository {
    suspend fun addInteraction(
        userId: UserId,
        listingId: ListingId,
        interactionType: InteractionType
    ): RepositoryOperationResult<Listing>

    suspend fun removeInteraction(
        userId: UserId,
        listingId: ListingId,
        interactionType: InteractionType
    ): RepositoryOperationResult<Listing>
}