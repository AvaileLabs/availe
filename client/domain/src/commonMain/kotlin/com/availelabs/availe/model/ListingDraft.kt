package com.availelabs.availe.model

import com.availelabs.availe.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class ListingDraft(
    val title: ListingTitle,
    val description: ListingDescription,
    val priceAmount: PriceAmount,
    val priceCurrencyCode: CurrencyCode,
    val imageUrl: ImageUrl,
    val startDate: LocalDate?,
    val startTime: LocalTime?,
    val endDate: LocalDate?,
    val endTime: LocalTime?,
    val timeZoneIdentifier: TimeZoneIdentifier,
    val type: ListingCategory,
    val stepIndex: Int
)