package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SupportedCurrency(val code: String, val symbol: String) {
    USD("USD", "$"),
    EUR("EUR", "€")
}

@Serializable
enum class ListingCategory {
    Item, Event, Service
}