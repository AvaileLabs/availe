package com.availelabs.availe.repositories.interaction

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.repositories.listing.ListingRepository
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.repositories.user.UserRepository
import com.availelabs.availe.testutils.PostgresTestBase
import com.example.jooq.enums.InteractionTypeEnum
import com.example.jooq.tables.references.EVENT_RSVPS
import com.example.jooq.tables.references.LISTINGS
import com.example.jooq.tables.references.LISTING_INTERACTIONS
import com.example.jooq.tables.references.USERS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class InteractionRepositoryTest : PostgresTestBase() {

    private lateinit var interactionRepository: InteractionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var listingRepository: ListingRepository

    @BeforeEach
    fun setupRepositories() {
        dsl.deleteFrom(EVENT_RSVPS).execute()
        dsl.deleteFrom(LISTING_INTERACTIONS).execute()
        dsl.deleteFrom(LISTINGS).execute()
        dsl.deleteFrom(USERS).execute()

        interactionRepository = InteractionRepository(dsl)
        userRepository = UserRepository(dsl)

        val itemCreator = ItemListingCreator(dsl)
        val eventCreator = EventListingCreator(dsl)
        val serviceCreator = ServiceListingCreator(dsl)
        listingRepository = ListingRepository(dsl, itemCreator, eventCreator, serviceCreator)
    }

    private fun createAndPersistUser(displayName: String): UserId {
        val user = userRepository.UpsertUser(Jid("$displayName@localhost"), Email("$displayName@example.com"))
        userRepository.UpdateProfile(user.id, UserDisplayName(displayName), null)
        return user.id
    }

    private fun createAndPersistItemListing(publisherId: UserId): ListingId {
        val publisher = userRepository.GetUserById(publisherId)!!

        val newListing = NewListing.Item(
            commonData = CommonListingData(
                title = ListingTitle("Test Item"),
                description = ListingDescription("Description"),
                price = Price(PriceAmount("10.00"), CurrencyCode("USD")),
                imageLinks = emptyList(),
                location = ListingLocation("Test Location")
            ),
            publisher = publisher
        )

        val result = listingRepository.createListing(newListing)
        return (result as InsertResult.Inserted).value.id
    }

    private fun createAndPersistEventListing(publisherId: UserId): ListingId {
        val publisher = userRepository.GetUserById(publisherId)!!

        val newListing = NewListing.Event(
            commonData = CommonListingData(
                title = ListingTitle("Test Event"),
                description = ListingDescription("Event Description"),
                price = Price(PriceAmount("20.00"), CurrencyCode("USD")),
                imageLinks = emptyList(),
                location = ListingLocation("Test Location")
            ),
            publisher = publisher,
            startTime = Clock.System.now()
        )

        val result = listingRepository.createListing(newListing)
        return (result as InsertResult.Inserted).value.id
    }

    @Test
    fun `addInteraction inserts a new favorite interaction`() {
        val publisherId = createAndPersistUser("Publisher")
        val listingId = createAndPersistItemListing(publisherId)
        val likerId = createAndPersistUser("Liker")

        interactionRepository.addInteraction(likerId, listingId, InteractionType.Favorite)

        val exists = dsl.fetchExists(
            dsl.selectOne()
                .from(LISTING_INTERACTIONS)
                .where(LISTING_INTERACTIONS.USER_ID.eq(likerId.value.toJavaUuid()))
                .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
                .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(InteractionTypeEnum.favorite))
        )
        assertTrue(exists)
    }

    @Test
    fun `addInteraction is idempotent for favorite`() {
        val publisherId = createAndPersistUser("Publisher")
        val listingId = createAndPersistItemListing(publisherId)
        val likerId = createAndPersistUser("Liker")

        interactionRepository.addInteraction(likerId, listingId, InteractionType.Favorite)
        interactionRepository.addInteraction(likerId, listingId, InteractionType.Favorite)

        val count = dsl.fetchCount(
            dsl.selectFrom(LISTING_INTERACTIONS)
                .where(LISTING_INTERACTIONS.USER_ID.eq(likerId.value.toJavaUuid()))
                .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
        )
        assertEquals(1, count)
    }

    @Test
    fun `removeInteraction deletes the favorite interaction`() {
        val publisherId = createAndPersistUser("Publisher")
        val listingId = createAndPersistItemListing(publisherId)
        val likerId = createAndPersistUser("Liker")

        interactionRepository.addInteraction(likerId, listingId, InteractionType.Favorite)
        interactionRepository.removeInteraction(likerId, listingId, InteractionType.Favorite)

        val exists = dsl.fetchExists(
            dsl.selectOne()
                .from(LISTING_INTERACTIONS)
                .where(LISTING_INTERACTIONS.USER_ID.eq(likerId.value.toJavaUuid()))
                .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
        )
        assertFalse(exists)
    }

    @Test
    fun `addInteraction inserts a new rsvp interaction`() {
        val publisherId = createAndPersistUser("Publisher")
        val listingId = createAndPersistEventListing(publisherId)
        val attendeeId = createAndPersistUser("Attendee")

        interactionRepository.addInteraction(attendeeId, listingId, InteractionType.Rsvp)

        val exists = dsl.fetchExists(
            dsl.selectOne()
                .from(EVENT_RSVPS)
                .where(EVENT_RSVPS.USER_ID.eq(attendeeId.value.toJavaUuid()))
                .and(EVENT_RSVPS.LISTING_ID.eq(listingId.value.toJavaUuid()))
        )
        assertTrue(exists)
    }

    @Test
    fun `removeInteraction deletes the rsvp interaction`() {
        val publisherId = createAndPersistUser("Publisher")
        val listingId = createAndPersistEventListing(publisherId)
        val attendeeId = createAndPersistUser("Attendee")

        interactionRepository.addInteraction(attendeeId, listingId, InteractionType.Rsvp)
        interactionRepository.removeInteraction(attendeeId, listingId, InteractionType.Rsvp)

        val exists = dsl.fetchExists(
            dsl.selectOne()
                .from(EVENT_RSVPS)
                .where(EVENT_RSVPS.USER_ID.eq(attendeeId.value.toJavaUuid()))
                .and(EVENT_RSVPS.LISTING_ID.eq(listingId.value.toJavaUuid()))
        )
        assertFalse(exists)
    }
}