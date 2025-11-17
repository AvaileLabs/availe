@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.data.network

import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.domain.model.CommonListingData
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IApiClient {
    suspend fun getListings(): List<Listing>
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
}

private fun PriceDto.V1.toDomainPrice(): Price = Price(
    amount = this.amount,
    currency = this.currency
)

private fun TypeV1ListingDtoData.toDomainListing(): Listing {
    val createdAt = Instant.DISTANT_PAST
    val stubbedPublisherId = Uuid.random()

    return when (this) {
        is ListingEventDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = stubbedPublisherId
            )
            Listing.Event(common, this.id, createdAt)
        }

        is ListingItemDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = stubbedPublisherId
            )
            Listing.Item(common, this.id, createdAt)
        }

        is ListingServiceDtoSchema.V1.Data -> {
            val common = CommonListingData(
                title = this.title,
                description = this.description,
                price = this.price.toDomainPrice(),
                imageLinks = this.imageLinks,
                publisherId = stubbedPublisherId
            )
            Listing.Service(common, this.id, createdAt)
        }
    }
}