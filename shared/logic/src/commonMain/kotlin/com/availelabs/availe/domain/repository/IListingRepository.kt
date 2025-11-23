@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.repository

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DbDeleteResult
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.domain.types.DbInsertResult
import com.availelabs.availe.domain.types.DbUpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi

interface IListingRepository {
    fun createListing(newListing: NewListing): DbInsertResult<Listing>
    fun getListingById(id: ListingId): DbFindResult<Listing>
    fun existsById(id: ListingId): Boolean
    fun getAllListings(): List<Listing>
    fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): DbUpdateResult<Listing>

    fun deleteListing(id: ListingId, requestingPublisherId: UserId): DbDeleteResult
}
