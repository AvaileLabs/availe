@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.services

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
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
private class FakeListingRepository(private val willInsert: Boolean) : IListingRepository {
    var received: NewListing? = null
    private val listings = mutableMapOf<ListingId, Listing>()

    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        received = newListing

        if (!willInsert) return InsertResult.AlreadyExists

        val fakeId = ListingId(Uuid.random())
        val fakeCreatedAt = ListingCreationTime(Instant.now().toKotlinInstant())
        val interactionState = InteractionState(isFavorited = false, isRsvped = false)

        val fakeDomainListing: Listing = when (newListing) {
            is NewListing.Event -> Listing.Event(
                newListing.commonData,
                fakeId,
                fakeCreatedAt,
                newListing.publisher,
                interactionState,
                newListing.startTime
            )

            is NewListing.Item -> Listing.Item(
                newListing.commonData,
                fakeId,
                fakeCreatedAt,
                newListing.publisher,
                interactionState
            )

            is NewListing.Service -> Listing.Service(
                newListing.commonData,
                fakeId,
                fakeCreatedAt,
                newListing.publisher,
                interactionState
            )
        }

        listings[fakeId] = fakeDomainListing
        return InsertResult.Inserted(fakeDomainListing)
    }

    override fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing> {
        return listings[id]?.let { FindResult.Found(it) } ?: FindResult.NotFound
    }

    override fun existsById(id: ListingId): Boolean {
        return listings.containsKey(id)
    }

    override fun getAllListings(viewingUserId: UserId?): List<Listing> {
        return listings.values.toList()
    }

    override fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId?,
        filter: ListingCategoryFilter,
        eventFeedFilter: EventFeedFilter
    ): List<Listing> {
        return listings.values.toList()
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        viewingUserId: UserId
    ): UpdateResult<Listing> {
        val existing = listings[id] ?: return UpdateResult.NotFound
        val currentCommon = existing.commonData

        val newCommonData = when (patch) {
            is ListingItemDtoSchema.V1.PatchRequest -> CommonListingData(
                title = patch.title.valueOrExisting(currentCommon.title),
                description = patch.description.valueOrExisting(currentCommon.description),
                price = patch.price.valueOrExisting(currentCommon.price),
                imageLinks = patch.imageLinks.valueOrExisting(currentCommon.imageLinks),
                location = patch.location.valueOrExisting(currentCommon.location)
            )

            is ListingEventDtoSchema.V1.PatchRequest -> CommonListingData(
                title = patch.title.valueOrExisting(currentCommon.title),
                description = patch.description.valueOrExisting(currentCommon.description),
                price = patch.price.valueOrExisting(currentCommon.price),
                imageLinks = patch.imageLinks.valueOrExisting(currentCommon.imageLinks),
                location = patch.location.valueOrExisting(currentCommon.location)
            )

            is ListingServiceDtoSchema.V1.PatchRequest -> CommonListingData(
                title = patch.title.valueOrExisting(currentCommon.title),
                description = patch.description.valueOrExisting(currentCommon.description),
                price = patch.price.valueOrExisting(currentCommon.price),
                imageLinks = patch.imageLinks.valueOrExisting(currentCommon.imageLinks),
                location = patch.location.valueOrExisting(currentCommon.location)
            )
        }

        val updatedListing = when (existing) {
            is Listing.Item -> existing.copy(commonData = newCommonData)
            is Listing.Event -> existing.copy(commonData = newCommonData)
            is Listing.Service -> existing.copy(commonData = newCommonData)
        }

        listings[id] = updatedListing
        return UpdateResult.Updated(updatedListing)
    }

    override fun deleteListing(id: ListingId): DeleteResult {
        return if (listings.remove(id) != null) DeleteResult.Deleted else DeleteResult.NotFound
    }
}

private fun aNewItem(): NewListing.Item {
    val publisherId = UserId(Uuid.random())
    val publisher = PublicProfile(
        id = publisherId,
        displayName = UserDisplayName("Test User"),
        avatarUrl = null,
        jid = Jid("test@xmpp")
    )
    return NewListing.Item(
        commonData = CommonListingData(
            title = ListingTitle("Bike"),
            description = ListingDescription("Blue road bike"),
            price = Price(PriceAmount("100.00"), CurrencyCode("USD")),
            imageLinks = emptyList(),
            location = ListingLocation("Test City")
        ),
        publisher = publisher
    )
}

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