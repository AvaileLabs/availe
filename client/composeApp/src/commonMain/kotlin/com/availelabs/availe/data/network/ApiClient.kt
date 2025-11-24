@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.auth.SessionManager
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
import kotlinx.coroutines.CancellationException
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
    private val json: Json,
    private val sessionManager: SessionManager
) : IApiClient {

    private suspend fun validateResponse(response: HttpResponse) {
        if (response.status == HttpStatusCode.Unauthorized) {
            sessionManager.onSessionInvalidated()
            throw CancellationException("Unauthorized access")
        }
        if (!response.status.isSuccess()) {
            throw Exception("${response.status} - ${response.bodyAsText()}")
        }
    }

    override suspend fun getListings(): List<Listing> {
        val response = httpClient.get(ApiRoutes.LISTINGS)
        validateResponse(response)
        val jsonString = response.bodyAsText()
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

        validateResponse(response)
        val jsonString = response.bodyAsText()
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

        validateResponse(response)
        val jsonString = response.bodyAsText()
        val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
        return responseDto.toDomainListing()
    }

    override suspend fun deleteListing(listingId: ListingId, publisherId: UserId) {
        val response = httpClient.delete("${ApiRoutes.LISTINGS}/${listingId.value}") {
            parameter("publisherId", publisherId.value.toString())
        }
        validateResponse(response)
    }
}