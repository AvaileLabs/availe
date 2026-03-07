package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.model.ListingDraft
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.Flow

interface IListingRepository {
    val updates: Flow<Unit>

    fun getListingsPageStream(
        page: Int,
        size: Int,
        forceRefresh: Boolean,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    ): Flow<DataStream<List<Listing>>>

    fun getListingByIdStream(id: ListingId): Flow<Listing?>

    suspend fun prefetchListings(
        page: Int,
        size: Int,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    )

    suspend fun fetchAndCacheListingById(id: ListingId): RepositoryOperationResult<Unit>
    suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean>
    suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>>
    suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>>

    suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult>
    suspend fun isListingCacheStale(
        filter: ListingCategoryFilter = ListingCategoryFilter.All,
        eventFeedFilter: EventFeedFilter = EventFeedFilter.None
    ): Boolean

    suspend fun saveDraft(draft: ListingDraft)
    suspend fun getDraft(): ListingDraft?
    suspend fun clearDraft()
}