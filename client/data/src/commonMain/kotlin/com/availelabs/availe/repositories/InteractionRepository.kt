package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.sources.local.IListingLocalDataSource
import com.availelabs.availe.sources.remote.IInteractionRemoteDataSource
import com.availelabs.availe.types.RepositoryOperationResult

internal class InteractionRepository(
    private val remoteDataSource: IInteractionRemoteDataSource,
    private val localDataSource: IListingLocalDataSource,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IInteractionRepository {

    override suspend fun addInteraction(
        userId: UserId,
        listingId: ListingId,
        interactionType: InteractionType
    ): RepositoryOperationResult<Unit> {
        val result = safeNetworkCaller.execute {
            remoteDataSource.addInteraction(listingId, interactionType)
        }

        if (result is RepositoryOperationResult.Success) {
            try {
                when (interactionType) {
                    InteractionType.Rsvp -> localDataSource.updateRsvpStatus(listingId, true)
                    InteractionType.Favorite -> localDataSource.updateFavoriteStatus(listingId, true)
                }
            } catch (_: Exception) {}
        }

        return result
    }

    override suspend fun removeInteraction(
        userId: UserId,
        listingId: ListingId,
        interactionType: InteractionType
    ): RepositoryOperationResult<Unit> {
        val result = safeNetworkCaller.execute {
            remoteDataSource.removeInteraction(listingId, interactionType)
        }

        if (result is RepositoryOperationResult.Success) {
            try {
                when (interactionType) {
                    InteractionType.Rsvp -> localDataSource.updateRsvpStatus(listingId, false)
                    InteractionType.Favorite -> localDataSource.updateFavoriteStatus(listingId, false)
                }
            } catch (_: Exception) {}
        }

        return result
    }
}