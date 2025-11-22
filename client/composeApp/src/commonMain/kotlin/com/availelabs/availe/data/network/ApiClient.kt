@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.ApiMediaTypes
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoCreateRequest
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
        println("Fetching listings from ${ApiRoutes.LISTINGS}")
        val response = httpClient.get(ApiRoutes.LISTINGS)
        val jsonString = response.bodyAsText()

        if (!response.status.isSuccess()) {
            println("Error fetching listings. Status: ${response.status}. Body: $jsonString")
            throw Exception("Failed to fetch listings: ${response.status} - $jsonString")
        }

        val dtos = json.decodeFromString(ListSerializer(TypeV1ListingDtoData.serializer()), jsonString)
        return dtos.map { it.toDomainListing() }
    }

    override suspend fun createListing(listing: NewListing, publisherId: Uuid): Listing {
        val dto = listing.toCreateRequestDto()
        val requestBody = json.encodeToString(TypeV1ListingDtoCreateRequest.serializer(), dto)

        println("Creating listing. Publisher: $publisherId")
        println("Request Body: $requestBody")

        val response = httpClient.post(ApiRoutes.LISTINGS) {
            header(HttpHeaders.ContentType, ApiMediaTypes.LISTINGS_V1_JSON)
            header(HttpHeaders.Accept, ApiMediaTypes.LISTINGS_V1_JSON)
            parameter("publisherId", publisherId.toString())
            setBody(requestBody)
        }

        val jsonString = response.bodyAsText()
        println("Create Listing Response Status: ${response.status}")
        println("Create Listing Response Body: $jsonString")

        if (!response.status.isSuccess()) {
            throw Exception("Creation failed: ${response.status} - $jsonString")
        }

        val responseDto = json.decodeFromString(TypeV1ListingDtoData.serializer(), jsonString)
        return responseDto.toDomainListing()
    }

    override suspend fun deleteListing(listingId: Uuid, publisherId: Uuid) {
        println("Deleting listing $listingId for publisher $publisherId")
        val response = httpClient.delete("${ApiRoutes.LISTINGS}/${listingId}") {
            parameter("publisherId", publisherId.toString())
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            println("Delete Listing Error: ${response.status} - $errorBody")
            throw Exception("Deletion failed: ${response.status} - $errorBody")
        }
    }
}