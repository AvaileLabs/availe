@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.controllers

import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.*
import com.availelabs.availe.mappers.toMonetaryAmount
import com.availelabs.availe.services.IListingService
import io.availe.models.SerializablePatchable
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path
import org.http4k.lens.RequestKey
import java.math.BigDecimal
import javax.money.MonetaryAmount
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val publisherKey = RequestKey.required<Uuid>("publisherId")

class ListingControllerV1(
    private val listingService: IListingService
) {
    private val listingIdPath = Path.kUuid().of("listingId")
    private val responseBodyLens = Body.auto<TypeV1ListingDtoData>().toLens()
    private val examplePrice = PriceDto.V1("10.00", "USD")
    private val exampleResponseItem = ListingItemDtoSchema.V1.Data(
        id = Uuid.random(),
        title = "Example Item",
        description = null,
        price = examplePrice,
        imageLinks = emptyList()
    )

    val listOfRoutes = listOf(createListing(), getListingById(), updateListing(), deleteListing())

    fun createListing(): ContractRoute {
        val bodyLens = Body.auto<TypeV1ListingDtoCreateRequest>().toLens()
        val exampleItem = ListingItemDtoSchema.V1.CreateRequest("Example Item", null, examplePrice, emptyList())

        return ApiRoutes.LISTINGS meta {
            summary = "Create a new listing."
            description =
                "Creates a new Item, Event, or Service listing. Publisher is derived from authorization filter."
            receiving(bodyLens to exampleItem)
            returning(Status.CREATED, responseBodyLens to exampleResponseItem)
            returning(Status.CONFLICT)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.POST to { req ->
            val publisherId = publisherKey(req)
            val newListing = when (val dto = bodyLens(req)) {
                is ListingEventDtoSchema.V1.CreateRequest -> NewListing.Event(dto.toCommonData(publisherId = publisherId))
                is ListingItemDtoSchema.V1.CreateRequest -> NewListing.Item(dto.toCommonData(publisherId = publisherId))
                is ListingServiceDtoSchema.V1.CreateRequest -> NewListing.Service(dto.toCommonData(publisherId = publisherId))
            }

            when (val result = listingService.createListing(newListing)) {
                is InsertResult.Inserted<Listing> -> {
                    val listing = result.value
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
            { _ ->
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

    fun updateListing(): ContractRoute {
        val patchBodyLens = Body.auto<TypeV1ListingDtoPatchRequest>().toLens()
        val exampleRequest = ListingItemDtoSchema.V1.PatchRequest(title = SerializablePatchable.Set("New Title"))

        return ApiRoutes.LISTINGS / listingIdPath meta {
            summary = "Update an existing listing."
            description = "Updates fields on a listing. Only authenticated publishers can update their own listings."
            receiving(patchBodyLens to exampleRequest)
            returning(Status.OK, responseBodyLens to exampleResponseItem)
            returning(Status.NOT_FOUND)
            returning(Status.FORBIDDEN)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.PATCH to { listingId ->
            { req ->
                val patchDto = patchBodyLens(req)
                val publisherId = publisherKey(req)

                when (val result = listingService.updateListing(listingId, patchDto, publisherId)) {
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
            { req ->
                val publisherId = publisherKey(req)

                when (listingService.deleteListing(listingId, publisherId)) {
                    is DeleteResult.Deleted -> Response(Status.NO_CONTENT)
                    is DeleteResult.NotFound -> Response(Status.NOT_FOUND)
                    is DeleteResult.Forbidden -> Response(Status.FORBIDDEN)
                }
            }
        }
    }
}

private fun ListingItemDtoSchema.V1.CreateRequest.toCommonData(publisherId: Uuid) =
    CommonListingData(
        title = title,
        description = description,
        price = price.toMonetaryAmount(),
        imageLinks = imageLinks,
        publisherId = publisherId
    )

private fun ListingEventDtoSchema.V1.CreateRequest.toCommonData(publisherId: Uuid) =
    CommonListingData(
        title = title,
        description = description,
        price = price.toMonetaryAmount(),
        imageLinks = imageLinks,
        publisherId = publisherId
    )

private fun ListingServiceDtoSchema.V1.CreateRequest.toCommonData(publisherId: Uuid) =
    CommonListingData(
        title = title,
        description = description,
        price = price.toMonetaryAmount(),
        imageLinks = imageLinks,
        publisherId = publisherId
    )

internal fun MonetaryAmount.toPriceDtoV1(): PriceDto.V1 = PriceDto.V1(
    amount = this.number.numberValue(BigDecimal::class.java).toPlainString(),
    currency = this.currency.currencyCode
)

internal fun Listing.toDataDto(): TypeV1ListingDtoData {
    val priceDto = this.price.toPriceDtoV1()
    return when (this) {
        is Listing.Event -> ListingEventDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            schemaVersion = 1
        )

        is Listing.Item -> ListingItemDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            schemaVersion = 1
        )

        is Listing.Service -> ListingServiceDtoSchema.V1.Data(
            id = this.id,
            title = this.title,
            description = this.description,
            price = priceDto,
            imageLinks = this.imageLinks,
            schemaVersion = 1
        )
    }
}

fun Path.kUuid() = map(Uuid::parse, Uuid::toString)