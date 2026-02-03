@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.sources.remote

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.extensions.ensureSuccess
import com.availelabs.availe.mappers.toCreateRequestDto
import com.availelabs.availe.mappers.toDomainListing
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

internal interface IListingRemoteDataSource {
    suspend fun getListingsPage(
        page: Int,
        size: Int,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter
    ): List<Listing>

    suspend fun getListingById(id: ListingId): FindResult<Listing>
    suspend fun existsById(id: ListingId): Boolean
    suspend fun createListing(listing: NewListing): InsertResult<Listing>
    suspend fun updateListing(
        listingId: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing>

    suspend fun deleteListing(listingId: ListingId): DeleteResult
}

internal class ListingRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IListingRemoteDataSource {

    override suspend fun getListingsPage(
        page: Int,
        size: Int,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing> {
        val response = httpClient.get(ApiRoutes.LISTINGS) {
            parameter(ApiParameters.PAGE_NUMBER, page)
            parameter(ApiParameters.PAGE_SIZE, size)
            if (publisherId != null) {
                parameter(ApiParameters.PUBLISHER_ID, publisherId.value.toString())
            }
            if (!filter.isAll) {
                filter.categories.forEach { category ->
                    parameter(ApiParameters.CATEGORIES, category.name)
                }
            }
        }
        response.ensureSuccess()
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
                response.ensureSuccess()
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
                response.ensureSuccess()
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
                response.ensureSuccess()
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
                response.ensureSuccess()
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
                response.ensureSuccess()
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }
}