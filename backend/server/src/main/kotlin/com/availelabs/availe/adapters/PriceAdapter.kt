package com.availelabs.availe.adapters

import com.availelabs.availe.domain.model.Price
import org.javamoney.moneta.Money
import java.math.BigDecimal
import javax.money.MonetaryAmount

internal fun Price.toMoneta(): MonetaryAmount = Money.parse("$currency $amount")

internal fun MonetaryAmount.toSharedPrice(): Price = Price(
    amount = this.number.numberValue(BigDecimal::class.java).toPlainString(),
    currency = this.currency.currencyCode
)