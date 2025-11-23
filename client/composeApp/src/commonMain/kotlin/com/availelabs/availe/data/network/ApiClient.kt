@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

interface IApiClient {
    suspend fun getListings(): List<Listing>
    suspend fun createListing(listing: NewListing, publisherId: UserId): Listing
    suspend fun updateListing(listingId: ListingId, patch: TypeV1ListingDtoPatchRequest, publisherId: UserId): Listing
    suspend fun deleteListing(listingId: ListingId, publisherId: UserId)
}

class ApiClient(
    private val httpClient: HttpClient,
    private val json: Json
) : IApiClient {

    override suspend fun getListings(): List<Listing> {
        val response = httpClient.get(ApiRoutes.LISTINGS)
        val jsonString = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch listings: ${response.status} - $jsonString")
        }

        val dtos = json.decodeFromString(ListSerializer(TypeV1ListingDtoData.serializer()), jsonString)
        return dtos.map { it.toDomainListing() }
    }

    override suspend fun createListing(listing: NewListing, publisherId: UserId): Listing {
        val dto = listing.toCreateRequestDto()
        val response = httpClient.post(ApiRoutes.LISTINGS) {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            parameter("publisherId", publisherId.value.toString())
            setBody(dto)
        }

        val jsonString = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw Exception("Creation failed: ${response.status} - $jsonString")
        }

        val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
        return responseDto.toDomainListing()
    }

    override suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        publisherId: UserId
    ): Listing {
        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}") {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            parameter("publisherId", publisherId.value.toString())
            setBody(patch)
        }

        val jsonString = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw Exception("Update failed: ${response.status} - $jsonString")
        }

        val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
        return responseDto.toDomainListing()
    }

    override suspend fun deleteListing(listingId: ListingId, publisherId: UserId) {
        val response = httpClient.delete("${ApiRoutes.LISTINGS}/${listingId.value}") {
            parameter("publisherId", publisherId.value.toString())
        }

        if (!response.status.isSuccess()) {
            throw Exception("Deletion failed: ${response.status} - ${response.bodyAsText()}")
        }
    }
}