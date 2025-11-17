@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.service

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IListingService {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: Uuid): FindResult<Listing>
    fun updateListing(
        listingId: Uuid,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: Uuid
    ): UpdateResult<Listing>

    fun deleteListing(listingId: Uuid, requestingPublisherId: Uuid): DeleteResult
}