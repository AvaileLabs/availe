@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.interaction

import com.availelabs.availe.domain.model.InteractionState
import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.example.jooq.enums.InteractionTypeEnum
import com.example.jooq.tables.references.EVENT_RSVPS
import com.example.jooq.tables.references.LISTING_INTERACTIONS
import org.jooq.DSLContext
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

interface IInteractionRepository {
    fun addInteraction(userId: UserId, listingId: ListingId, type: InteractionType)
    fun removeInteraction(userId: UserId, listingId: ListingId, type: InteractionType)
    fun getUserInteractions(userId: UserId, listingIdentifiers: List<ListingId>): Map<ListingId, InteractionState>
    fun getListingIdentifiersWithRsvp(userId: UserId): List<ListingId>
}

class InteractionRepository(
    private val databaseContext: DSLContext
) : IInteractionRepository {

    override fun addInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
        when (type) {
            InteractionType.Favorite -> {
                databaseContext.insertInto(LISTING_INTERACTIONS)
                    .set(LISTING_INTERACTIONS.USER_ID, userId.value.toJavaUuid())
                    .set(LISTING_INTERACTIONS.LISTING_ID, listingId.value.toJavaUuid())
                    .set(LISTING_INTERACTIONS.INTERACTION_TYPE, InteractionTypeEnum.favorite)
                    .onConflictDoNothing()
                    .execute()
            }

            InteractionType.Rsvp -> {
                databaseContext.insertInto(EVENT_RSVPS)
                    .set(EVENT_RSVPS.USER_ID, userId.value.toJavaUuid())
                    .set(EVENT_RSVPS.LISTING_ID, listingId.value.toJavaUuid())
                    .onConflictDoNothing()
                    .execute()
            }
        }
    }

    override fun removeInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
        when (type) {
            InteractionType.Favorite -> {
                databaseContext.deleteFrom(LISTING_INTERACTIONS)
                    .where(LISTING_INTERACTIONS.USER_ID.eq(userId.value.toJavaUuid()))
                    .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
                    .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(InteractionTypeEnum.favorite))
                    .execute()
            }

            InteractionType.Rsvp -> {
                databaseContext.deleteFrom(EVENT_RSVPS)
                    .where(EVENT_RSVPS.USER_ID.eq(userId.value.toJavaUuid()))
                    .and(EVENT_RSVPS.LISTING_ID.eq(listingId.value.toJavaUuid()))
                    .execute()
            }
        }
    }

    override fun getUserInteractions(
        userId: UserId,
        listingIdentifiers: List<ListingId>
    ): Map<ListingId, InteractionState> {
        if (listingIdentifiers.isEmpty()) {
            return emptyMap()
        }

        val javaUuidListingIdentifiers = listingIdentifiers.map { it.value.toJavaUuid() }

        val favoritedListingIdentifiers = databaseContext.select(LISTING_INTERACTIONS.LISTING_ID)
            .from(LISTING_INTERACTIONS)
            .where(LISTING_INTERACTIONS.USER_ID.eq(userId.value.toJavaUuid()))
            .and(LISTING_INTERACTIONS.LISTING_ID.`in`(javaUuidListingIdentifiers))
            .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(InteractionTypeEnum.favorite))
            .fetchInto(UUID::class.java)
            .map { ListingId(it.toKotlinUuid()) }
            .toSet()

        val rsvpedListingIdentifiers = databaseContext.select(EVENT_RSVPS.LISTING_ID)
            .from(EVENT_RSVPS)
            .where(EVENT_RSVPS.USER_ID.eq(userId.value.toJavaUuid()))
            .and(EVENT_RSVPS.LISTING_ID.`in`(javaUuidListingIdentifiers))
            .fetchInto(UUID::class.java)
            .map { ListingId(it.toKotlinUuid()) }
            .toSet()

        val interactionStateMap = mutableMapOf<ListingId, InteractionState>()

        for (listingIdentifier in listingIdentifiers) {
            val interactionState = InteractionState(
                isFavorited = favoritedListingIdentifiers.contains(listingIdentifier),
                isRsvped = rsvpedListingIdentifiers.contains(listingIdentifier)
            )
            interactionStateMap[listingIdentifier] = interactionState
        }

        return interactionStateMap
    }

    override fun getListingIdentifiersWithRsvp(userId: UserId): List<ListingId> {
        return databaseContext.select(EVENT_RSVPS.LISTING_ID)
            .from(EVENT_RSVPS)
            .where(EVENT_RSVPS.USER_ID.eq(userId.value.toJavaUuid()))
            .fetchInto(UUID::class.java)
            .map { ListingId(it.toKotlinUuid()) }
    }
}