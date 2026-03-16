@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.controllers

import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.auth.OptionalAuthenticationFilter
import com.availelabs.availe.auth.SecurityContexts
import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.mappers.toDataDto
import com.availelabs.availe.service.IEventService
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Query
import org.http4k.lens.enum
import org.http4k.lens.int
import org.http4k.lens.uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class EventController(
    private val eventService: IEventService,
    private val optionalAuthenticationFilter: OptionalAuthenticationFilter
) {
    private val listResponseBodyLens = Body.auto<List<ListingEventDtoSchema.V1.Data>>().toLens()
    private val pageNumberQueryLens = Query.int().defaulted(ApiParameters.PAGE_NUMBER, 0)
    private val pageSizeQueryLens = Query.int().defaulted(ApiParameters.PAGE_SIZE, 20)
    private val publisherIdQueryLens = Query.uuid()
        .map({ UserId(it.toKotlinUuid()) }, { it.value.toJavaUuid() })
        .optional(ApiParameters.PUBLISHER_ID)
    private val eventFeedFilterQueryLens =
        Query.enum<EventFeedFilter>().defaulted("eventFeedFilter", EventFeedFilter.None)

    val listOfRoutes = listOf(
        getEventFeed()
    )

    fun getEventFeed(): ContractRoute {
        return ApiRoutes.EVENTS meta {
            summary = "Get event feed."
            description = "Returns a paginated list of event listings, filtered by temporal states."
            queries += pageNumberQueryLens
            queries += pageSizeQueryLens
            queries += publisherIdQueryLens
            queries += eventFeedFilterQueryLens
            returning(Status.OK)
        } bindContract Method.GET to optionalAuthenticationFilter.then { request: Request ->
            val pageNumber = pageNumberQueryLens(request)
            val pageSize = pageSizeQueryLens(request)
            val publisherId = publisherIdQueryLens(request)
            val eventFeedFilter = eventFeedFilterQueryLens(request)
            val viewingUser = SecurityContexts.optionalAuthenticatedUserLens(request)

            val eventListings = eventService.getEventFeedPage(
                pageNumber = pageNumber,
                pageSize = pageSize,
                viewingUserId = viewingUser?.id,
                publisherId = publisherId,
                eventFeedFilter = eventFeedFilter
            )

            val responseDataTransferObjects = eventListings.map { it.toDataDto() as ListingEventDtoSchema.V1.Data }
            Response(Status.OK).with(listResponseBodyLens of responseDataTransferObjects)
        }
    }
}