@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories.listing

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.testutils.PostgresTestBase
import io.availe.models.Patchable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ListingRepositoryTest : PostgresTestBase() {

    private lateinit var repository: ListingRepository
    private val testPublisherId = UserId(Uuid.random())

    private fun aNewItemListing(
        price: Price = Price(PriceAmount("100.50"), CurrencyCode("USD")),
        description: ListingDescription? = ListingDescription("Initial description"),
        title: ListingTitle = ListingTitle("Initial Title"),
        imageLinks: List<ImageUrl> = listOf(ImageUrl("initial.png"), ImageUrl("header.jpg"), ImageUrl("thumbnail.jpg"))
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
                title = ListingTitle("Initial Event"),
                description = ListingDescription("Initial event description"),
                price = Price(PriceAmount("25.00"), CurrencyCode("USD")),
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

    private suspend fun insertListing(newListing: NewListing): Listing {
        val result = repository.createListing(newListing)
        assertTrue(result is InsertResult.Inserted, "Setup failed: Could not insert listing")
        return (result as InsertResult.Inserted).value
    }

    @Test
    fun `updateListing updates a single title field`() {
        runBlockingTest {
            val initialListing = insertListing(aNewItemListing(title = ListingTitle("Old Title")))
            val newTitle = ListingTitle("New Title")
            val patch = ListingItemDtoSchema.V1.PatchRequest(title = Patchable.Set(newTitle))

            val result = repository.updateListing(initialListing.id, patch)

            assertTrue(result is UpdateResult.Updated)
            val updatedListing = (result as UpdateResult.Updated).value
            assertEquals(newTitle, updatedListing.title)
            assertEquals(initialListing.description, updatedListing.description)
            assertEquals(initialListing.price, updatedListing.price)
        }
    }

    @Test
    fun `updateListing updates price field`() {
        runBlockingTest {
            val initialListing =
                insertListing(aNewItemListing(price = Price(PriceAmount("100.00"), CurrencyCode("USD"))))
            val newPrice = Price(PriceAmount("99.99"), CurrencyCode("EUR"))
            val patch = ListingItemDtoSchema.V1.PatchRequest(price = Patchable.Set(newPrice))

            val result = repository.updateListing(initialListing.id, patch)

            assertTrue(result is UpdateResult.Updated)
            val updatedListing = (result as UpdateResult.Updated).value
            assertEquals(newPrice, updatedListing.price)
            assertNotEquals(initialListing.price, updatedListing.price)
            assertEquals(initialListing.title, updatedListing.title)
        }
    }

    @Test
    fun `updateListing updates description to null`() {
        runBlockingTest {
            val initialListing = insertListing(aNewItemListing(description = ListingDescription("Not null")))
            val patch = ListingItemDtoSchema.V1.PatchRequest(description = Patchable.Set(null))

            val result = repository.updateListing(initialListing.id, patch)

            assertTrue(result is UpdateResult.Updated)
            val updatedListing = (result as UpdateResult.Updated).value
            assertNull(updatedListing.description)
            assertNotEquals(initialListing.description, updatedListing.description)
        }
    }

    @Test
    fun `updateListing updates multiple fields at once`() {
        runBlockingTest {
            val initialListing = insertListing(
                aNewItemListing(
                    title = ListingTitle("Old Title"),
                    imageLinks = listOf(ImageUrl("old.png"))
                )
            )
            val newTitle = ListingTitle("New Title")
            val newImageLinks = listOf(ImageUrl("new1.png"), ImageUrl("new2.png"))

            val patch = ListingItemDtoSchema.V1.PatchRequest(
                title = Patchable.Set(newTitle),
                imageLinks = Patchable.Set(newImageLinks)
            )

            val result = repository.updateListing(initialListing.id, patch)

            assertTrue(result is UpdateResult.Updated)
            val updatedListing = (result as UpdateResult.Updated).value
            assertEquals(newTitle, updatedListing.title)
            assertEquals(newImageLinks, updatedListing.imageLinks)
            assertEquals(initialListing.description, updatedListing.description)
        }
    }

    @Test
    fun `updateListing with empty patch returns Updated and changes nothing`() {
        runBlockingTest {
            val initialListing = insertListing(aNewItemListing())
            val emptyPatch = ListingItemDtoSchema.V1.PatchRequest()

            val result = repository.updateListing(initialListing.id, emptyPatch)

            assertTrue(result is UpdateResult.Updated)
            val updatedListing = (result as UpdateResult.Updated).value
            assertEquals(initialListing, updatedListing)
        }
    }

    @Test
    fun `updateListing on non-existent ID returns NotFound`() {
        runBlockingTest {
            val randomId = ListingId(Uuid.random())
            val patch = ListingItemDtoSchema.V1.PatchRequest(title = Patchable.Set(ListingTitle("New Title")))

            val result = repository.updateListing(randomId, patch)

            assertTrue(result is UpdateResult.NotFound)
        }
    }

    @Test
    fun `updateListing works for Event listing type`() {
        runBlockingTest {
            val initialEvent = insertListing(aNewEventListing())
            val newTitle = ListingTitle("Updated Event Title")
            val patch = ListingEventDtoSchema.V1.PatchRequest(title = Patchable.Set(newTitle))

            val result = repository.updateListing(initialEvent.id, patch)

            assertTrue(result is UpdateResult.Updated)
            val updatedEvent = (result as UpdateResult.Updated).value
            assertEquals(newTitle, updatedEvent.title)
            assertEquals(initialEvent.description, updatedEvent.description)
        }
    }

    @Test
    fun `deleteListing returns Deleted when listing exists`() {
        runBlockingTest {
            val initialListing = insertListing(aNewItemListing())

            val result = repository.deleteListing(initialListing.id)

            assertTrue(result is DeleteResult.Deleted)
            val findResult = repository.getListingById(initialListing.id)
            assertTrue(findResult is FindResult.NotFound)
        }
    }

    @Test
    fun `deleteListing returns NotFound when listing does not exist`() {
        runBlockingTest {
            val randomId = ListingId(Uuid.random())

            val result = repository.deleteListing(randomId)

            assertTrue(result is DeleteResult.NotFound)
        }
    }

    @Test
    fun `getListingById fetches correctly after creation`() {
        runBlockingTest {
            val initialListing = insertListing(aNewItemListing())

            val findResult = repository.getListingById(initialListing.id)

            assertTrue(findResult is FindResult.Found)
            val foundListing = (findResult as FindResult.Found).value
            assertEquals(initialListing, foundListing)
        }
    }

    @Test
    fun `getListingById returns NotFound for non-existent ID`() {
        runBlockingTest {
            val findResult = repository.getListingById(ListingId(Uuid.random()))
            assertTrue(findResult is FindResult.NotFound)
        }
    }

    private fun runBlockingTest(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }
}