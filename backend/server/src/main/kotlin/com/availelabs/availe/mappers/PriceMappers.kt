package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.CurrencyCode
import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.domain.model.PriceAmount
import com.availelabs.availe.domain.model.SupportedCurrency
import java.math.BigDecimal
import javax.money.Monetary
import javax.money.MonetaryAmount

internal fun Price.toMoneta(): MonetaryAmount {
    val validCurrency = SupportedCurrency.fromCode(this.currency)
        ?: throw IllegalArgumentException("Unsupported currency: ${this.currency.value}")

    return Monetary.getDefaultAmountFactory()
        .setCurrency(validCurrency.code.value)
        .setNumber(BigDecimal(this.amount.value))
        .create()
}

internal fun MonetaryAmount.toSharedPrice(): Price = Price(
    amount = PriceAmount(this.number.numberValue(BigDecimal::class.java).toPlainString()),
    currency = CurrencyCode(this.currency.currencyCode)
)