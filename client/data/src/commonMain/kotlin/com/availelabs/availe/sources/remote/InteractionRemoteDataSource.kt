@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.sources.remote

import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.InteractionAction
import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.dto.kreplica.InteractionRequestDtoSchema
import com.availelabs.availe.extensions.ensureSuccess
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi

internal interface IInteractionRemoteDataSource {
    suspend fun addInteraction(listingId: ListingId, interactionType: InteractionType)
    suspend fun removeInteraction(listingId: ListingId, interactionType: InteractionType)
}

internal class InteractionRemoteDataSource(
    private val httpClient: HttpClient
) : IInteractionRemoteDataSource {

    override suspend fun addInteraction(listingId: ListingId, interactionType: InteractionType) {
        val requestBody = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Add,
            type = interactionType
        )

        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}/interaction") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestBody)
        }

        response.ensureSuccess()
    }

    override suspend fun removeInteraction(listingId: ListingId, interactionType: InteractionType) {
        val requestBody = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Remove,
            type = interactionType
        )

        val response = httpClient.patch("${ApiRoutes.LISTINGS}/${listingId.value}/interaction") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(requestBody)
        }

        response.ensureSuccess()
    }
}