@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.service

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

interface IListingService {
    suspend fun createListing(newListing: NewListing): InsertResult<Listing>
    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun getAllListings(): List<Listing>
    suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): UpdateResult<Listing>

    suspend fun deleteListing(listingId: ListingId, requestingPublisherId: UserId): DeleteResult
}