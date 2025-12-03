@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.data.sources.remote

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.mappers.toCreateRequestDto
import com.availelabs.availe.mappers.toDomainListing
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

interface IListingRemoteDataSource {
    suspend fun getListingsPage(page: Int, size: Int): List<Listing>
    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun existsById(id: ListingId): Boolean
    suspend fun createListing(listing: NewListing): InsertResult<Listing>
    suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing>

    suspend fun deleteListing(listingId: ListingId): DeleteResult
}

class ListingRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IListingRemoteDataSource {

    override suspend fun getListingsPage(page: Int, size: Int): List<Listing> {
        val response = httpClient.get(ApiRoutes.LISTINGS) {
            parameter(ApiParameters.PAGE_NUMBER, page)
            parameter(ApiParameters.PAGE_SIZE, size)
        }
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

    override suspend fun existsById(id: ListingId): Boolean {
        val response = httpClient.get("${ApiRoutes.LISTINGS}/${id.value}")

        return when (response.status) {
            HttpStatusCode.OK -> true
            HttpStatusCode.NotFound -> false
            else -> {
                ensureSuccess(response)
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    override suspend fun createListing(listing: NewListing): InsertResult<Listing> {
        val dto = listing.toCreateRequestDto()
        val response = httpClient.post(ApiRoutes.LISTINGS) {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
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
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}") {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
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

    override suspend fun deleteListing(listingId: ListingId): DeleteResult {
        val response = httpClient.delete("${ApiRoutes.LISTINGS}/${listingId.value}")

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

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw Exception("${response.status} - ${response.bodyAsText()}")
        }
    }
}