package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SupportedCurrency(val code: CurrencyCode, val symbol: String) {
    USD(CurrencyCode("USD"), "$"),
    EUR(CurrencyCode("EUR"), "€");

    companion object {
        fun fromCode(code: CurrencyCode): SupportedCurrency? = entries.find { it.code == code }
    }
}

@Serializable
enum class ListingCategory {
    Item, Event, Service
}

@Serializable
enum class InteractionType {
    Favorite
}

@Serializable
enum class InteractionAction {
    Add, Remove
}