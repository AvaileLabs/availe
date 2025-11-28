package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Price(
    val amount: PriceAmount,
    val currency: CurrencyCode
)