package com.availelabs.availe.dto

interface PriceDto {
    data class V1(
        val amount: String,
        val currency: String
    ) : PriceDto
}