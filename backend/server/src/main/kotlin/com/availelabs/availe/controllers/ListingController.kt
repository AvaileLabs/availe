package com.availelabs.availe.controllers

import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.SecurityContexts
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoCreateRequest
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.mappers.toDataDto
import com.availelabs.availe.mappers.toDomain
import com.availelabs.availe.observability.ListingCreated
import com.availelabs.availe.service.IListingService
import io.availe.models.Patchable
import kotlinx.coroutines.runBlocking
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.events.Events
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.uuid
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
internal class ListingController(
    private val listingService: IListingService,
    private val events: Events,
    private val authenticationFilter: AuthenticationFilter
) {
    private val listingIdPath =
        Path.uuid().map({ ListingId(it.toKotlinUuid()) }, { it.value.toJavaUuid() }).of(ApiParameters.LISTING_ID)
    private val responseBodyLens = Body.auto<TypeV1ListingDtoData>().toLens()
    private val listResponseBodyLens = Body.auto<List<TypeV1ListingDtoData>>().toLens()
    private val pageNumberQueryLens = Query.int().defaulted("page", 0)
    private val pageSizeQueryLens = Query.int().defaulted("size", 20)

    private val examplePrice = Price(PriceAmount("10.00"), CurrencyCode("USD"))
    private val exampleResponseItem = ListingItemDtoSchema.V1.Data(
        id = ListingId(Uuid.random()),
        title = ListingTitle("Example Item"),
        description = null,
        price = examplePrice,
        imageLinks = emptyList(),
        publisherId = UserId(Uuid.random()),
    )

    val listOfRoutes = listOf(
        createListing(),
        getListingById(),
        checkListingExists(),
        listAllListings(),
        updateListing(),
        deleteListing()
    )

    fun createListing(): ContractRoute {
        val bodyLens = Body.auto<TypeV1ListingDtoCreateRequest>().toLens()
        val exampleItem =
            ListingItemDtoSchema.V1.CreateRequest(ListingTitle("Example Item"), null, examplePrice, emptyList())

        return ApiRoutes.LISTINGS meta {
            summary = "Create a new listing."
            description =
                "Creates a new Item, Event, or Service listing. Publisher is derived from authorization filter."
            receiving(bodyLens to exampleItem)
            returning(Status.CREATED, responseBodyLens to exampleResponseItem)
            returning(Status.CONFLICT)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.POST to authenticationFilter.then { request ->
            val authenticatedUser = SecurityContexts.authenticatedUserLens(request)
            val newListing = bodyLens(request).toDomain(authenticatedUser.id)

            runBlocking {
                when (val result = listingService.createListing(newListing)) {
                    is InsertResult.Inserted<Listing> -> {
                        val listing = result.value
                        events(ListingCreated(listing.id.value.toString(), authenticatedUser.id.value.toString()))
                        val responseDto = listing.toDataDto()
                        Response(Status.CREATED).with(responseBodyLens of responseDto)
                    }

                    is InsertResult.AlreadyExists -> Response(Status.CONFLICT)
                }
            }
        }
    }

    fun getListingById(): ContractRoute {
        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Get a listing by its ID."
            description = "Returns a single Item, Event, or Service listing."
            returning(Status.OK, responseBodyLens to exampleResponseItem)
            returning(Status.NOT_FOUND)
        } bindContract Method.GET to { listingId ->
            { _: Request ->
                runBlocking {
                    when (val result = listingService.getListingById(listingId)) {
                        is FindResult.Found -> {
                            val responseDto = result.value.toDataDto()
                            Response(Status.OK).with(responseBodyLens of responseDto)
                        }

                        is FindResult.NotFound -> Response(Status.NOT_FOUND)
                    }
                }
            }
        }
    }

    fun checkListingExists(): ContractRoute {
        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Check if a listing exists."
            description = "Returns 200 OK if the listing exists, 404 Not Found otherwise."
            returning(Status.OK)
            returning(Status.NOT_FOUND)
        } bindContract Method.HEAD to { listingId ->
            { _: Request ->
                runBlocking {
                    val exists = try {
                        val result = listingService.getListingById(listingId)
                        result is FindResult.Found
                    } catch (_: Exception) {
                        false
                    }

                    if (exists) {
                        Response(Status.OK)
                    } else {
                        Response(Status.NOT_FOUND)
                    }
                }
            }
        }
    }

    fun listAllListings(): ContractRoute {
        return ApiRoutes.LISTINGS meta {
            summary = "Get all listings."
            description = "Returns a paginated list of all listings."
            queries += pageNumberQueryLens
            queries += pageSizeQueryLens
            returning(Status.OK, listResponseBodyLens to listOf(exampleResponseItem))
        } bindContract Method.GET to { request: Request ->
            runBlocking {
                val pageNumber = pageNumberQueryLens(request)
                val pageSize = pageSizeQueryLens(request)
                val listings = listingService.getListingsPage(pageNumber, pageSize)
                val responseDto = listings.map { it.toDataDto() }
                Response(Status.OK).with(listResponseBodyLens of responseDto)
            }
        }
    }

    fun updateListing(): ContractRoute {
        val patchBodyLens = Body.auto<TypeV1ListingDtoPatchRequest>().toLens()
        val exampleRequest = ListingItemDtoSchema.V1.PatchRequest(title = Patchable.Set(ListingTitle("New Title")))

        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Update an existing listing."
            description = "Updates fields on a listing. Only authenticated publishers can update their own listings."
            receiving(patchBodyLens to exampleRequest)
            returning(Status.OK, responseBodyLens to exampleResponseItem)
            returning(Status.NOT_FOUND)
            returning(Status.FORBIDDEN)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.PATCH to { listingId ->
            authenticationFilter.then { request ->
                val patchDto = patchBodyLens(request)
                val authenticatedUser = SecurityContexts.authenticatedUserLens(request)

                when (val result = listingService.updateListing(listingId, patchDto, authenticatedUser.id)) {
                    is UpdateResult.Updated -> {
                        val responseDto = result.value.toDataDto()
                        Response(Status.OK).with(responseBodyLens of responseDto)
                    }

                    is UpdateResult.NotFound -> Response(Status.NOT_FOUND)
                    is UpdateResult.Forbidden -> Response(Status.FORBIDDEN)
                }

            }
        }
    }

    fun deleteListing(): ContractRoute {
        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Delete a listing."
            description = "Deletes a listing. Only authenticated publishers can delete their own listings."
            returning(Status.NO_CONTENT)
            returning(Status.NOT_FOUND)
            returning(Status.FORBIDDEN)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.DELETE to { listingId ->
            authenticationFilter.then { request ->
                val authenticatedUser = SecurityContexts.authenticatedUserLens(request)

                when (listingService.deleteListing(listingId, authenticatedUser.id)) {
                    is DeleteResult.Deleted -> Response(Status.NO_CONTENT)
                    is DeleteResult.NotFound -> Response(Status.NOT_FOUND)
                    is DeleteResult.Forbidden -> Response(Status.FORBIDDEN)
                }

            }
        }
    }
}