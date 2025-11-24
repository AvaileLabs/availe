package com.availelabs.availe.domain

import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.domain.model.SupportedCurrency

fun Price.toDisplayString(): String {
    val supportedCurrency = SupportedCurrency.entries.find { it.code == this.currency }
    val symbol = supportedCurrency?.symbol ?: this.currency
    return "$symbol${this.amount.value}"
}