@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories.listing

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.availelabs.availe.domain.listing.NewListing
import com.availelabs.availe.domain.types.DbDeleteResult
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.domain.types.DbInsertResult
import com.availelabs.availe.domain.types.DbUpdateResult
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.testutils.PostgresTestBase
import io.availe.models.SerializablePatchable
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import javax.money.MonetaryAmount
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ListingRepositoryTest : PostgresTestBase() {

    private lateinit var repository: ListingRepository
    private val testPublisherId = Uuid.random()
    private val otherPublisherId = Uuid.random()

    private fun aNewItemListing(
        price: MonetaryAmount = Money.of(100.50, "USD"),
        description: String? = "Initial description",
        title: String = "Initial Title",
        imageLinks: List<String> = listOf("initial.png", "header.jpg", "thumbnail.jpg")
    ): NewListing.Item {
        return NewListing.Item(
            CommonListingData(
                title = title,
                description = description,
                price = price,
                imageLinks = imageLinks,
                publisherId = testPublisherId
            )
        )
    }

    private fun aNewEventListing(): NewListing.Event {
        return NewListing.Event(
            CommonListingData(
                title = "Initial Event",
                description = "Initial event description",
                price = Money.of(25, "USD"),
                imageLinks = emptyList(),
                publisherId = testPublisherId
            )
        )
    }

    @BeforeEach
    fun setupRepository() {
        val itemCreator = ItemListingCreator(dsl)
        val eventCreator = EventListingCreator(dsl)
        val serviceCreator = ServiceListingCreator(dsl)

        repository = ListingRepository(
            dsl = dsl,
            itemCreator = itemCreator,
            eventCreator = eventCreator,
            serviceCreator = serviceCreator
        )
    }

    private fun insertListing(newListing: NewListing): Listing {
        val result = repository.createListing(newListing)
        assertTrue(result is DbInsertResult.Inserted, "Setup failed: Could not insert listing")
        return (result as DbInsertResult.Inserted).value
    }

    @Test
    fun `updateListing updates a single title field`() {
        val initialListing = insertListing(aNewItemListing(title = "Old Title"))
        val newTitle = "New Title"
        val patch = ListingItemDtoSchema.V1.PatchRequest(title = SerializablePatchable.Set(newTitle))

        val result = repository.updateListing(initialListing.id, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedListing = (result as DbUpdateResult.Updated).value
        assertEquals(newTitle, updatedListing.title)
        assertEquals(initialListing.description, updatedListing.description)
        assertEquals(initialListing.price, updatedListing.price)
    }

    @Test
    fun `updateListing updates price field`() {
        val initialListing = insertListing(aNewItemListing(price = Money.of(100, "USD")))
        val newPriceDto = PriceDto.V1("99.99", "EUR")
        val expectedPriceMoney = Money.of(BigDecimal("99.99"), "EUR")
        val patch = ListingItemDtoSchema.V1.PatchRequest(price = SerializablePatchable.Set(newPriceDto))

        val result = repository.updateListing(initialListing.id, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedListing = (result as DbUpdateResult.Updated).value
        assertEquals(expectedPriceMoney, updatedListing.price)
        assertNotEquals(initialListing.price, updatedListing.price)
        assertEquals(initialListing.title, updatedListing.title)
    }

    @Test
    fun `updateListing updates description to null`() {
        val initialListing = insertListing(aNewItemListing(description = "Not null"))
        val patch = ListingItemDtoSchema.V1.PatchRequest(description = SerializablePatchable.Set(null))

        val result = repository.updateListing(initialListing.id, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedListing = (result as DbUpdateResult.Updated).value
        assertNull(updatedListing.description)
        assertNotEquals(initialListing.description, updatedListing.description)
    }

    @Test
    fun `updateListing updates multiple fields at once`() {
        val initialListing = insertListing(
            aNewItemListing(
                title = "Old Title",
                imageLinks = listOf("old.png")
            )
        )
        val newTitle = "New Title"
        val newImageLinks = listOf("new1.png", "new2.png")

        val patch = ListingItemDtoSchema.V1.PatchRequest(
            title = SerializablePatchable.Set(newTitle),
            imageLinks = SerializablePatchable.Set(newImageLinks)
        )

        val result = repository.updateListing(initialListing.id, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedListing = (result as DbUpdateResult.Updated).value
        assertEquals(newTitle, updatedListing.title)
        assertEquals(newImageLinks, updatedListing.imageLinks)
        assertEquals(initialListing.description, updatedListing.description)
    }

    @Test
    fun `updateListing with empty patch returns Updated and changes nothing`() {
        val initialListing = insertListing(aNewItemListing())
        val emptyPatch = ListingItemDtoSchema.V1.PatchRequest()

        val result = repository.updateListing(initialListing.id, emptyPatch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedListing = (result as DbUpdateResult.Updated).value
        assertEquals(initialListing, updatedListing)
    }

    @Test
    fun `updateListing on non-existent ID returns NotFound`() {
        val randomId = Uuid.random()
        val patch = ListingItemDtoSchema.V1.PatchRequest(title = SerializablePatchable.Set("New Title"))

        val result = repository.updateListing(randomId, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.NotFound)
    }

    @Test
    fun `updateListing works for Event listing type`() {
        val initialEvent = insertListing(aNewEventListing())
        val newTitle = "Updated Event Title"
        val patch = ListingEventDtoSchema.V1.PatchRequest(title = SerializablePatchable.Set(newTitle))

        val result = repository.updateListing(initialEvent.id, patch, testPublisherId)

        assertTrue(result is DbUpdateResult.Updated)
        val updatedEvent = (result as DbUpdateResult.Updated).value
        assertEquals(newTitle, updatedEvent.title)
        assertEquals(initialEvent.description, updatedEvent.description)
    }

    @Test
    fun `updateListing returns NotFound when publisherId does not match`() {
        val initialListing = insertListing(aNewItemListing())
        val patch = ListingItemDtoSchema.V1.PatchRequest(title = SerializablePatchable.Set("New Title"))

        val result = repository.updateListing(initialListing.id, patch, otherPublisherId)

        assertTrue(result is DbUpdateResult.NotFound)
    }

    @Test
    fun `updateListing with empty patch returns NotFound when publisherId does not match`() {
        val initialListing = insertListing(aNewItemListing())
        val emptyPatch = ListingItemDtoSchema.V1.PatchRequest()

        val result = repository.updateListing(initialListing.id, emptyPatch, otherPublisherId)

        assertTrue(result is DbUpdateResult.NotFound)
    }

    @Test
    fun `deleteListing returns Deleted when publisherId matches`() {
        val initialListing = insertListing(aNewItemListing())

        val result = repository.deleteListing(initialListing.id, testPublisherId)

        assertTrue(result is DbDeleteResult.Deleted)
        val findResult = repository.getListingById(initialListing.id)
        assertTrue(findResult is DbFindResult.NotFound)
    }

    @Test
    fun `deleteListing returns NotFound when publisherId does not match`() {
        val initialListing = insertListing(aNewItemListing())

        val result = repository.deleteListing(initialListing.id, otherPublisherId)

        assertTrue(result is DbDeleteResult.NotFound)
        val findResult = repository.getListingById(initialListing.id)
        assertTrue(findResult is DbFindResult.Found)
    }

    @Test
    fun `getListingById fetches correctly after creation`() {
        val initialListing = insertListing(aNewItemListing())

        val findResult = repository.getListingById(initialListing.id)

        assertTrue(findResult is DbFindResult.Found)
        val foundListing = (findResult as DbFindResult.Found).value
        assertEquals(initialListing, foundListing)
    }

    @Test
    fun `getListingById returns NotFound for non-existent ID`() {
        val findResult = repository.getListingById(Uuid.random())
        assertTrue(findResult is DbFindResult.NotFound)
    }
}