package com.availelabs.availe.data.network

import com.availelabs.availe.ApiMediaTypes.LISTINGS_V1_JSON
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class ListingApiService(
    private val http: HttpClient,
    private val json: Json,
) : ApiResponseValidator {
    val listingsV1JsonContentType: ContentType =
        ContentType.parse(LISTINGS_V1_JSON)

    suspend fun getListings(): List<TypeV1ListingDtoData> {
        val response = http.get(ApiRoutes.LISTINGS)
        validateResponse(response)
        return response.body()
    }

    suspend fun upsertListing(listing: TypeV1ListingDtoData): TypeV1ListingDtoData {
        val response = http.post(ApiRoutes.LISTINGS) {
            contentType(listingsV1JsonContentType)
            accept(listingsV1JsonContentType)
            setBody(listing)
        }
        validateResponse(response)
        return response.body()
    }

    suspend fun deleteListing(listingId: String): DeleteResult {
        val response = http.delete("${ApiRoutes.LISTINGS}/$listingId")

        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.NoContent -> DeleteResult.Deleted
            HttpStatusCode.NotFound -> DeleteResult.NotFound
            HttpStatusCode.Forbidden -> DeleteResult.Forbidden
            else -> response.throwUnexpected()
        }
    }
}

interface ApiResponseValidator {
    suspend fun validateResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            response.throwUnexpected()
        }
    }
}

internal suspend fun HttpResponse.throwUnexpected(): Nothing {
    throw Exception("$status - ${bodyAsText()}")
}