@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.service.ListingService
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import kotlinx.coroutines.runBlocking
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
    override suspend fun createListing(newListing: NewListing): InsertResult<Listing> {
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

    override suspend fun getListingById(id: ListingId): FindResult<Listing> {
        TODO("Not yet implemented")
    }

    override suspend fun existsById(id: ListingId): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getAllListings(): List<Listing> {
        TODO("Not yet implemented")
    }

    override suspend fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteListing(id: ListingId): DeleteResult {
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

        val result = runBlocking { service.createListing(newItem) }

        assertTrue(result is InsertResult.Inserted)
        assertEquals(newItem, fakeRepo.received)
    }

    @Test
    fun `returns already exists when repository returns already exists`() {
        val fakeRepo = FakeListingRepository(willInsert = false)
        val service = ListingService(fakeRepo)
        val newItem = aNewItem()

        val result = runBlocking { service.createListing(newItem) }

        assertTrue(result is InsertResult.AlreadyExists)
        assertEquals(newItem, fakeRepo.received)
    }
}