@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.interaction

import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.example.jooq.enums.InteractionTypeEnum
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
        val typeEnum = when (type) {
            InteractionType.Favorite -> InteractionTypeEnum.favorite
        }

        dsl.insertInto(LISTING_INTERACTIONS)
            .set(LISTING_INTERACTIONS.USER_ID, userId.value.toJavaUuid())
            .set(LISTING_INTERACTIONS.LISTING_ID, listingId.value.toJavaUuid())
            .set(LISTING_INTERACTIONS.INTERACTION_TYPE, typeEnum)
            .onConflictDoNothing()
            .execute()
    }

    override fun removeInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
        val typeEnum = when (type) {
            InteractionType.Favorite -> InteractionTypeEnum.favorite
        }

        dsl.deleteFrom(LISTING_INTERACTIONS)
            .where(LISTING_INTERACTIONS.USER_ID.eq(userId.value.toJavaUuid()))
            .and(LISTING_INTERACTIONS.LISTING_ID.eq(listingId.value.toJavaUuid()))
            .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(typeEnum))
            .execute()
    }
}