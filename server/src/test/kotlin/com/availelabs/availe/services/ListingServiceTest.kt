@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.repositories.listing.IListingRepository
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
private class FakeListingRepository(private val willInsert: Boolean) : IListingRepository {
    var received: NewListing? = null
    override fun createListing(newListing: NewListing): InsertResult {
        received = newListing

        if (!willInsert) return InsertResult.AlreadyExists

        val fakeId = Uuid.random()
        val fakeCreatedAt = Instant.now().toKotlinInstant()

        val fakeDomainListing: Listing = when (newListing) {
            is NewListing.Event -> Listing.Event(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Item -> Listing.Item(newListing.commonData, fakeId, fakeCreatedAt)
            is NewListing.Service -> Listing.Service(newListing.commonData, fakeId, fakeCreatedAt)
        }

        return InsertResult.Inserted(fakeDomainListing)
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
    fun `returns inserted whenRepository inserts`() {
        TODO()
    }
}