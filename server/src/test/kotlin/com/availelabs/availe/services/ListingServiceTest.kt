@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.repositories.IListingRepository
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private class FakeListingRepository(private val willInsert: Boolean) : IListingRepository {
    var received: NewListing? = null
    override fun createListing(newListing: NewListing): Boolean {
        received = newListing
        return willInsert
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