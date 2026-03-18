package com.availelabs.availe.sources.remote

import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.extensions.ensureSuccess
import com.availelabs.availe.mappers.toDomainListing
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

internal interface IEventRemoteDataSource {
    suspend fun getEventFeedPage(
        page: Int,
        size: Int,
        publisherId: UserId? = null,
        eventFeedFilter: EventFeedFilter
    ): List<Listing.Event>
}

internal class EventRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IEventRemoteDataSource {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getEventFeedPage(
        page: Int,
        size: Int,
        publisherId: UserId?,
        eventFeedFilter: EventFeedFilter
    ): List<Listing.Event> {
        val response = httpClient.get(ApiRoutes.EVENTS) {
            parameter(ApiParameters.PAGE_NUMBER, page)
            parameter(ApiParameters.PAGE_SIZE, size)
            if (publisherId != null) {
                parameter(ApiParameters.PUBLISHER_ID, publisherId.value.toString())
            }
            parameter(ApiParameters.EVENT_FEED_FILTER, eventFeedFilter.name)
        }
        response.ensureSuccess()
        val jsonString = response.bodyAsText()
        val dataTransferObjects =
            json.decodeFromString(ListSerializer(ListingEventDtoSchema.V1.Data.serializer()), jsonString)
        return dataTransferObjects.map { it.toDomainListing() as Listing.Event }
    }
}