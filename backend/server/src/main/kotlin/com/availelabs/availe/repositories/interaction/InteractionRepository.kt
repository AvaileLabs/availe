@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.interaction

import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.example.jooq.enums.InteractionTypeEnum
import com.example.jooq.tables.references.EVENT_RSVPS
import com.example.jooq.tables.references.LISTING_INTERACTIONS
import org.jooq.DSLContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

interface IInteractionRepository {
    fun addInteraction(userId: UserId, listingId: ListingId, type: InteractionType)
    fun removeInteraction(userId: UserId, listingId: ListingId, type: InteractionType)
}

class InteractionRepository(
    private val dsl: DSLContext
) : IInteractionRepository {

    override fun addInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
        when (type) {
            InteractionType.Favorite -> {
                dsl.insertInto(LISTING_INTERACTIONS)
                    .set(LISTING_INTERACTIONS.USER_ID, userId.value.toJavaUuid())
                    .set(LISTING_INTERACTIONS.LISTING_ID, listingId.value.toJavaUuid())
                    .set(LISTING_INTERACTIONS.INTERACTION_TYPE, InteractionTypeEnum.favorite)
                    .onConflictDoNothing()
                    .execute()
            }
            InteractionType.Rsvp -> {
                dsl.insertInto(EVENT_RSVPS)
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
                dsl.deleteFrom(LISTING_INTERACTIONS)
                    .where(LISTING_INTERACTIONS.USER_ID.eq(userId.value.toJavaUuid()))
                    .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
                    .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(InteractionTypeEnum.favorite))
                    .execute()
            }
            InteractionType.Rsvp -> {
                dsl.deleteFrom(EVENT_RSVPS)
                    .where(EVENT_RSVPS.USER_ID.eq(userId.value.toJavaUuid()))
                    .and(EVENT_RSVPS.LISTING_ID.eq(listingId.value.toJavaUuid()))
                    .execute()
            }
        }
    }
}