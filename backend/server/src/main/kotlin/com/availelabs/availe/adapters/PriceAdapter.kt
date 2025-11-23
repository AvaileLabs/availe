package com.availelabs.availe.adapters

import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.domain.model.PriceAmount
import com.availelabs.availe.domain.model.SupportedCurrency
import org.javamoney.moneta.Money
import java.math.BigDecimal
import javax.money.MonetaryAmount

internal fun Price.toMoneta(): MonetaryAmount {
    val validCurrency = SupportedCurrency.entries.find { it.code == this.currency }
        ?: throw IllegalArgumentException("Unsupported currency: ${this.currency}")

    return Money.parse("${validCurrency.code} ${this.amount.value}")
}

internal fun MonetaryAmount.toSharedPrice(): Price = Price(
    amount = PriceAmount(this.number.numberValue(BigDecimal::class.java).toPlainString()),
    currency = this.currency.currencyCode
)