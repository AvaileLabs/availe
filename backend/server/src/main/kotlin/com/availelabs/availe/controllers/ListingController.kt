package com.availelabs.availe.controllers

import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.auth.AuthenticatedUser
import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.ITokenVerifier
import com.availelabs.availe.auth.SecurityContexts
import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.*
import com.availelabs.availe.mappers.toDataDto
import com.availelabs.availe.mappers.toDomain
import com.availelabs.availe.observability.ListingCreated
import com.availelabs.availe.service.IListingService
import io.availe.models.Patchable
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.events.Events
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.*
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
internal class ListingController(
    private val listingService: IListingService,
    private val events: Events,
    private val authenticationFilter: AuthenticationFilter,
    private val tokenVerifier: ITokenVerifier
) {
    private val listingIdPath =
        Path.uuid().map({ ListingId(it.toKotlinUuid()) }, { it.value.toJavaUuid() }).of(ApiParameters.LISTING_ID)
    private val responseBodyLens = Body.auto<TypeV1ListingDtoData>().toLens()
    private val listResponseBodyLens = Body.auto<List<TypeV1ListingDtoData>>().toLens()
    private val pageNumberQueryLens = Query.int().defaulted(ApiParameters.PAGE_NUMBER, 0)
    private val pageSizeQueryLens = Query.int().defaulted(ApiParameters.PAGE_SIZE, 20)
    private val publisherIdQueryLens = Query.uuid()
        .map({ UserId(it.toKotlinUuid()) }, { it.value.toJavaUuid() })
        .optional(ApiParameters.PUBLISHER_ID)
    private val categoriesQueryLens = Query.enum<ListingCategory>().multi.optional(ApiParameters.CATEGORIES)

    private val examplePrice = Price(PriceAmount("10.00"), CurrencyCode("USD"))
    private val examplePublisher = PublicProfileDtoSchema.V1.Data(
        id = UserId(Uuid.random()),
        displayName = UserDisplayName("Alice Seller"),
        avatarUrl = null,
        jid = Jid("alice@localhost")
    )
    private val exampleInteraction = InteractionStateDtoSchema.V1.Data(isFavorited = false)
    private val exampleResponseItem = ListingItemDtoSchema.V1.Data(
        id = ListingId(Uuid.random()),
        title = ListingTitle("Example Item"),
        description = null,
        price = examplePrice,
        imageLinks = emptyList(),
        location = ListingLocation("New York, NY"),
        publisher = examplePublisher,
        interactionState = exampleInteraction
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
            ListingItemDtoSchema.V1.CreateRequest(
                ListingTitle("Example Item"),
                null,
                examplePrice,
                emptyList(),
                ListingLocation("New York, NY")
            )

        return ApiRoutes.LISTINGS meta {
            summary = "Create a new listing."
            description =
                "Creates a new Item, Event, or Service listing. Publisher is derived from authorization filter."
            receiving(bodyLens to exampleItem)
            returning(Status.CREATED, responseBodyLens to exampleResponseItem)
            returning(Status.CONFLICT)
            returning(Status.UNAUTHORIZED)
            returning(Status.BAD_REQUEST)
        } bindContract Method.POST to authenticationFilter.then { request ->
            val authenticatedUser = SecurityContexts.authenticatedUserLens(request)

            val publisherProfile = PublicProfile(
                id = authenticatedUser.id,
                displayName = authenticatedUser.displayName,
                avatarUrl = authenticatedUser.avatarUrl,
                jid = authenticatedUser.jid ?: Jid("")
            )

            val newListing = bodyLens(request).toDomain(publisherProfile)

            if (!newListing.title.isValid()) {
                return@then Response(Status.BAD_REQUEST).body("Invalid Title")
            }
            if (!newListing.price.amount.isValid()) {
                return@then Response(Status.BAD_REQUEST).body("Invalid Price")
            }
            if (!newListing.imageLinks.isValid()) {
                return@then Response(Status.BAD_REQUEST).body("Invalid or missing Images")
            }
            if (!newListing.location.isValid()) {
                return@then Response(Status.BAD_REQUEST).body("Invalid Location")
            }

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

    fun getListingById(): ContractRoute {
        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Get a listing by its ID."
            description = "Returns a single Item, Event, or Service listing."
            returning(Status.OK, responseBodyLens to exampleResponseItem)
            returning(Status.NOT_FOUND)
        } bindContract Method.GET to { listingId ->
            { request: Request ->
                val viewingUser = tryAuthenticate(request)

                when (val result = listingService.getListingById(listingId, viewingUser?.id)) {
                    is FindResult.Found -> {
                        val responseDto = result.value.toDataDto()
                        Response(Status.OK).with(responseBodyLens of responseDto)
                    }

                    is FindResult.NotFound -> Response(Status.NOT_FOUND)
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
                val exists = try {
                    val result = listingService.getListingById(listingId, null)
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

    fun listAllListings(): ContractRoute {
        return ApiRoutes.LISTINGS meta {
            summary = "Get all listings."
            description =
                "Returns a paginated list of all listings, optionally filtered by publisher ID and/or categories."
            queries += pageNumberQueryLens
            queries += pageSizeQueryLens
            queries += publisherIdQueryLens
            queries += categoriesQueryLens
            returning(Status.OK, listResponseBodyLens to listOf(exampleResponseItem))
        } bindContract Method.GET to { request: Request ->
            val pageNumber = pageNumberQueryLens(request)
            val pageSize = pageSizeQueryLens(request)
            val publisherId = publisherIdQueryLens(request)
            val categoriesList = categoriesQueryLens(request)

            val viewingUser = tryAuthenticate(request)

            val filter = if (categoriesList.isNullOrEmpty()) {
                ListingCategoryFilter.All
            } else {
                ListingCategoryFilter.of(*categoriesList.toTypedArray())
            }

            val listings = listingService.getListingsPage(pageNumber, pageSize, viewingUser?.id, publisherId, filter)
            val responseDto = listings.map { it.toDataDto() }
            Response(Status.OK).with(listResponseBodyLens of responseDto)
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

    private fun tryAuthenticate(request: Request): AuthenticatedUser? {
        val authorizationHeader = request.header("Authorization") ?: return null
        val bearerPrefix = "Bearer "
        if (!authorizationHeader.startsWith(bearerPrefix)) return null

        val token = authorizationHeader.substringAfter(bearerPrefix)
        val verifiedToken = tokenVerifier.verify(token) ?: return null

        return AuthenticatedUser(
            id = verifiedToken.userId,
            email = verifiedToken.email,
            displayName = verifiedToken.displayName,
            avatarUrl = verifiedToken.avatarUrl,
            jid = verifiedToken.jid,
            subject = verifiedToken.subject
        )
    }
}