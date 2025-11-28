package com.availelabs.availe.domain

import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.domain.model.SupportedCurrency

fun Price.toDisplayString(): String {
    val supportedCurrency = SupportedCurrency.fromCode(this.currency)
    val symbol = supportedCurrency?.symbol ?: this.currency.value
    return "$symbol${this.amount.value}"
}