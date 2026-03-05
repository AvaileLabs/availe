@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.sources.remote

import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.InteractionAction
import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.dto.kreplica.InteractionRequestDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.extensions.ensureSuccess
import com.availelabs.availe.mappers.toDomainListing
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

internal interface IInteractionRemoteDataSource {
    suspend fun addInteraction(listingId: ListingId, interactionType: InteractionType): Listing
    suspend fun removeInteraction(listingId: ListingId, interactionType: InteractionType): Listing
}

internal class InteractionRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IInteractionRemoteDataSource {

    override suspend fun addInteraction(listingId: ListingId, interactionType: InteractionType): Listing {
        val requestBody = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Add,
            type = interactionType
        )

        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}/interaction") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestBody)
        }

        response.ensureSuccess()

        val responseJsonString = response.bodyAsText()
        val listingDataTransferObject = json.decodeFromString(TypeV1ListingDtoData.serializer(), responseJsonString)
        return listingDataTransferObject.toDomainListing()
    }

    override suspend fun removeInteraction(listingId: ListingId, interactionType: InteractionType): Listing {
        val requestBody = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Remove,
            type = interactionType
        )

        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}/interaction") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestBody)
        }

        response.ensureSuccess()

        val responseJsonString = response.bodyAsText()
        val listingDataTransferObject = json.decodeFromString(TypeV1ListingDtoData.serializer(), responseJsonString)
        return listingDataTransferObject.toDomainListing()
    }
}