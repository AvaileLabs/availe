package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.model.DataStream
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.flow.Flow

interface IListingRepository {
    val updates: Flow<Unit>

    fun getListingsPageStream(
        page: Int,
        size: Int,
        forceRefresh: Boolean,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    ): Flow<DataStream<List<Listing>>>

    suspend fun prefetchListings(
        page: Int,
        size: Int,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    )

    suspend fun getListingById(id: ListingId): RepositoryOperationResult<FindResult<Listing>>
    suspend fun existsById(id: ListingId): RepositoryOperationResult<Boolean>
    suspend fun createListing(newListing: NewListing): RepositoryOperationResult<InsertResult<Listing>>
    suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): RepositoryOperationResult<UpdateResult<Listing>>

    suspend fun deleteListing(id: ListingId): RepositoryOperationResult<DeleteResult>
    suspend fun isListingCacheStale(filter: ListingCategoryFilter = ListingCategoryFilter.All): Boolean
}