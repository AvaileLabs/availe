@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.repository

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

interface IListingRepository {
    suspend fun createListing(newListing: NewListing): InsertResult<Listing>
    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun existsById(id: ListingId): Boolean
    suspend fun getAllListings(): List<Listing>
    suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing>

    suspend fun deleteListing(id: ListingId): DeleteResult
}