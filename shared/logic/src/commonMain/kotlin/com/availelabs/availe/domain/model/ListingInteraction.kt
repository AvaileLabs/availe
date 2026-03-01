package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ListingInteraction(
    val userId: UserId,
    val listingId: ListingId,
    val type: InteractionType,
    val createdAt: Instant
)

@Serializable
data class InteractionState(
    val isFavorited: Boolean,
    val isRsvped: Boolean
)