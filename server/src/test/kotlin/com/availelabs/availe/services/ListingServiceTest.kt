@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.*
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.IListingRepository
import org.javamoney.moneta.Money
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

        val fakeId = Uuid.random()
        val fakeCreatedAt = Instant.now().toKotlinInstant()

        val fakeDomainListing: Listing = when (newListing) {
            is NewListing.Event -> Listing.Event(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Item -> Listing.Item(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Service -> Listing.Service(newListing.commonData, fakeId, fakeCreatedAt)
        }

        return DbInsertResult.Inserted(fakeDomainListing)
    }

    override fun getListingById(id: Uuid): DbFindResult<Listing> {
        TODO("Not yet implemented")
    }

    override fun getAllListings(): List<Listing> {
        TODO("Not yet implemented")
    }

    override fun updateListing(id: Uuid, patch: TypeV1ListingDtoPatchRequest): DbUpdateResult<Listing> {
        TODO("Not yet implemented")
    }

    override fun deleteListing(id: Uuid): DbDeleteResult {
        TODO("Not yet implemented")
    }
}

private fun aNewItem(): NewListing.Item =
    NewListing.Item(
        CommonListingData(
            title = "Bike",
            description = "Blue road bike",
            price = Money.of(100, "USD"),
            imageLinks = emptyList(),
            publisherId = Uuid.random()
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