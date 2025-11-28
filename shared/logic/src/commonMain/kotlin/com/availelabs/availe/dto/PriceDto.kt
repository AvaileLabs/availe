package com.availelabs.availe.dto

import com.availelabs.availe.domain.model.CurrencyCode
import com.availelabs.availe.domain.model.PriceAmount
import kotlinx.serialization.Serializable

interface PriceDto {
    @Serializable
    data class V1(
        val amount: PriceAmount,
        val currency: CurrencyCode
    ) : PriceDto
}