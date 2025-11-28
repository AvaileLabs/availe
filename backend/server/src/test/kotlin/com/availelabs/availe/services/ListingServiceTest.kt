@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.service.ListingService
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
private class FakeListingRepository(private val willInsert: Boolean) : IListingRepository {
    var received: NewListing? = null
    override fun createListing(newListing: NewListing): DbInsertResult<Listing> {
        received = newListing

        if (!willInsert) return DbInsertResult.AlreadyExists

        val fakeId = ListingId(Uuid.random())
        val fakeCreatedAt = Instant.now().toKotlinInstant()

        val fakeDomainListing: Listing = when (newListing) {
            is NewListing.Event -> Listing.Event(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Item -> Listing.Item(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Service -> Listing.Service(newListing.commonData, fakeId, fakeCreatedAt)
        }

        return DbInsertResult.Inserted(fakeDomainListing)
    }

    override fun getListingById(id: ListingId): DbFindResult<Listing> {
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
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): DbUpdateResult<Listing> {
        TODO("Not yet implemented")
    }

    override fun deleteListing(id: ListingId, requestingPublisherId: UserId): DbDeleteResult {
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
        val fakeRepo = FakeListingRepository(willInsert = true)
        val service = ListingService(fakeRepo)
        val newItem = aNewItem()

        val result = service.createListing(newItem)

        assertTrue(result is InsertResult.Inserted)
        assertEquals(newItem, fakeRepo.received)
    }

    @Test
    fun `returns already exists when repository returns already exists`() {
        val fakeRepo = FakeListingRepository(willInsert = false)
        val service = ListingService(fakeRepo)
        val newItem = aNewItem()

        val result = service.createListing(newItem)

        assertTrue(result is InsertResult.AlreadyExists)
        assertEquals(newItem, fakeRepo.received)
    }
}