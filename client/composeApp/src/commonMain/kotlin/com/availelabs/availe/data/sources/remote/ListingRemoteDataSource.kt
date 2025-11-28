@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.data.sources.remote

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.data.network.toCreateRequestDto
import com.availelabs.availe.data.network.toDomainListing
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

interface IListingRemoteDataSource {
    suspend fun getListings(): List<Listing>
    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun createListing(listing: NewListing, publisherId: UserId): InsertResult<Listing>
    suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        publisherId: UserId
    ): UpdateResult<Listing>

    suspend fun deleteListing(listingId: ListingId, publisherId: UserId): DeleteResult
}

class ListingRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IListingRemoteDataSource {

    override suspend fun getListings(): List<Listing> {
        val response = httpClient.get(ApiRoutes.LISTINGS)
        ensureSuccess(response)
        val jsonString = response.bodyAsText()
        val dtos = json.decodeFromString(ListSerializer(TypeV1ListingDtoData.serializer()), jsonString)
        return dtos.map { it.toDomainListing() }
    }

    override suspend fun getListingById(id: ListingId): FindResult<Listing> {
        val response = httpClient.get("${ApiRoutes.LISTINGS}/${id.value}")

        return when (response.status) {
            HttpStatusCode.OK -> {
                val jsonString = response.bodyAsText()
                val dto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
                FindResult.Found(dto.toDomainListing())
            }

            HttpStatusCode.NotFound -> FindResult.NotFound
            else -> {
                ensureSuccess(response)
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    override suspend fun createListing(listing: NewListing, publisherId: UserId): InsertResult<Listing> {
        val dto = listing.toCreateRequestDto()
        val response = httpClient.post(ApiRoutes.LISTINGS) {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            setPublisherId(publisherId)
            setBody(dto)
        }

        return when (response.status) {
            HttpStatusCode.Created -> {
                val jsonString = response.bodyAsText()
                val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
                InsertResult.Inserted(responseDto.toDomainListing())
            }

            HttpStatusCode.Conflict -> InsertResult.AlreadyExists
            else -> {
                ensureSuccess(response)
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    override suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        publisherId: UserId
    ): UpdateResult<Listing> {
        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}") {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            setPublisherId(publisherId)
            setBody(patch)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val jsonString = response.bodyAsText()
                val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
                UpdateResult.Updated(responseDto.toDomainListing())
            }

            HttpStatusCode.NotFound -> UpdateResult.NotFound
            HttpStatusCode.Forbidden -> UpdateResult.Forbidden
            else -> {
                ensureSuccess(response)
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    override suspend fun deleteListing(listingId: ListingId, publisherId: UserId): DeleteResult {
        val response = httpClient.delete("${ApiRoutes.LISTINGS}/${listingId.value}") {
            setPublisherId(publisherId)
        }

        return when (response.status) {
            HttpStatusCode.NoContent, HttpStatusCode.OK -> DeleteResult.Deleted
            HttpStatusCode.NotFound -> DeleteResult.NotFound
            HttpStatusCode.Forbidden -> DeleteResult.Forbidden
            else -> {
                ensureSuccess(response)
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    private fun HttpRequestBuilder.setPublisherId(publisherId: UserId) {
        parameter(ApiParameters.PUBLISHER_ID, publisherId.value.toString())
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw Exception("${response.status} - ${response.bodyAsText()}")
        }
    }
}