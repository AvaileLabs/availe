package com.availelabs.availe.model

import com.availelabs.availe.domain.model.*
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ListingDraft(
    val title: ListingTitle,
    val description: ListingDescription,
    val priceAmount: PriceAmount,
    val priceCurrencyCode: CurrencyCode,
    val imageUrl: ImageUrl,
    val startTime: Instant?,
    val type: ListingCategory,
    val stepIndex: Int
)