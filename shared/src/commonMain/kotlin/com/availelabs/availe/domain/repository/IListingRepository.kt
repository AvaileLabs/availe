@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.repository

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.DbDeleteResult
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.domain.types.DbInsertResult
import com.availelabs.availe.domain.types.DbUpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IListingRepository {
    fun createListing(newListing: NewListing): DbInsertResult<Listing>
    fun getListingById(id: Uuid): DbFindResult<Listing>
    fun existsById(id: Uuid): Boolean
    fun getAllListings(): List<Listing>
    fun updateListing(
        id: Uuid,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: Uuid
    ): DbUpdateResult<Listing>

    fun deleteListing(id: Uuid, requestingPublisherId: Uuid): DbDeleteResult
}