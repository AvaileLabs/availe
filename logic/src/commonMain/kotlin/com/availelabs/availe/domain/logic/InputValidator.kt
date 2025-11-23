package com.availelabs.availe.domain.logic

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.ListingTitle
import com.availelabs.availe.domain.model.PriceAmount

object InputValidator {
    const val MAX_TITLE_LENGTH = 100

    fun isValidPrice(input: PriceAmount): Boolean {
        if (input.value.isEmpty()) return true
        return input.value.all { it.isDigit() || it == '.' } && input.value.count { it == '.' } <= 1
    }

    fun isValidTitle(input: ListingTitle): Boolean {
        return input.value.isNotBlank() && input.value.length <= MAX_TITLE_LENGTH
    }

    fun isValidUrl(input: ImageUrl): Boolean {
        return input.value.isNotBlank()
    }
}