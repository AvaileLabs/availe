@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.IListingRepository
import com.availelabs.availe.service.ListingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
private class FakeListingRepositoryA(private val willInsert: Boolean) : IListingRepository {
    var received: NewListing? = null
    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        received = newListing

        if (!willInsert) return InsertResult.AlreadyExists

        val fakeId = ListingId(Uuid.random())
        val fakeCreatedAt = Instant.now().toKotlinInstant()

        val fakeDomainListing: Listing = when (newListing) {
            is NewListing.Event -> Listing.Event(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Item -> Listing.Item(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Service -> Listing.Service(newListing.commonData, fakeId, fakeCreatedAt)
        }

        return InsertResult.Inserted(fakeDomainListing)
    }

    override fun getListingById(id: ListingId): FindResult<Listing> {
        TODO("Not yet implemented")
    }

    override fun existsById(id: ListingId): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAllListings(): List<Listing> {
        TODO("Not yet implemented")
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
        TODO("Not yet implemented")
    }

    override fun deleteListing(id: ListingId): DeleteResult {
        TODO("Not yet implemented")
    }
}

private fun aNewItem(): NewListing.Item =
    NewListing.Item(
        CommonListingData(
            title = ListingTitle("Bike"),
            description = ListingDescription("Blue road bike"),
            price = Price(PriceAmount("100.00"), CurrencyCode("USD")),
            imageLinks = emptyList(),
            publisherId = UserId(Uuid.random())
        )
    )

class ListingServiceTest {
    @Test
    fun `returns inserted when repository inserts`() {
        val fakeRepo = FakeListingRepositoryA(willInsert = true)
        val service = ListingService(fakeRepo)
        val newItem = aNewItem()

        val result = service.createListing(newItem)

        assertTrue(result is InsertResult.Inserted)
        assertEquals(newItem, fakeRepo.received)
    }

    @Test
    fun `returns already exists when repository returns already exists`() {
        val fakeRepo = FakeListingRepositoryA(willInsert = false)
        val service = ListingService(fakeRepo)
        val newItem = aNewItem()

        val result = service.createListing(newItem)

        assertTrue(result is InsertResult.AlreadyExists)
        assertEquals(newItem, fakeRepo.received)
    }
}