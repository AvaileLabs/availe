package com.availelabs.availe.mappers

import com.availelabs.availe.domain.model.CurrencyCode
import com.availelabs.availe.domain.model.Price
import com.availelabs.availe.domain.model.PriceAmount
import com.availelabs.availe.domain.model.SupportedCurrency
import org.javamoney.moneta.Money
import java.math.BigDecimal
import javax.money.MonetaryAmount

internal fun Price.toMoneta(): MonetaryAmount {
    val validCurrency = SupportedCurrency.fromCode(this.currency)
        ?: throw IllegalArgumentException("Unsupported currency: ${this.currency.value}")

    return Money.parse("${validCurrency.code.value} ${this.amount.value}")
}

internal fun MonetaryAmount.toSharedPrice(): Price = Price(
    amount = PriceAmount(this.number.numberValue(BigDecimal::class.java).toPlainString()),
    currency = CurrencyCode(this.currency.currencyCode)
)