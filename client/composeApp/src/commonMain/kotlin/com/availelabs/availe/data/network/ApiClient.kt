@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IApiClient {
    suspend fun getListings(): List<Listing>
    suspend fun createListing(listing: NewListing, publisherId: Uuid): Listing
    suspend fun deleteListing(listingId: Uuid, publisherId: Uuid)
}

class ApiClient(
    private val httpClient: HttpClient,
    private val json: Json
) : IApiClient {

    override suspend fun getListings(): List<Listing> {
        val jsonString = httpClient.get(ApiRoutes.LISTINGS).bodyAsText()
        val dtos = json.decodeFromString(ListSerializer(TypeV1ListingDtoData.serializer()), jsonString)
        return dtos.map { it.toDomainListing() }
    }

    override suspend fun createListing(listing: NewListing, publisherId: Uuid): Listing {
        val dto = listing.toCreateRequestDto()
        val jsonString = httpClient.post(ApiRoutes.LISTINGS) {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            parameter("publisherId", publisherId.toString())
            setBody(dto)
        }.bodyAsText()

        val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
        return responseDto.toDomainListing()
    }

    override suspend fun deleteListing(listingId: Uuid, publisherId: Uuid) {
        httpClient.delete("${ApiRoutes.LISTINGS}/${listingId}") {
            parameter("publisherId", publisherId.toString())
        }
    }
}