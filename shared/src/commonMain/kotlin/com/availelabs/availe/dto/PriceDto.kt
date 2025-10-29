package com.availelabs.availe.dto

import kotlinx.serialization.Serializable

interface PriceDto {
    @Serializable
    data class V1(
        val amount: String,
        val currency: String
    ) : PriceDto
}