package com.availelabs.availe.domain.extensions

import com.availelabs.availe.domain.model.*

private const val MAXIMUM_TITLE_LENGTH = 100

fun Listing.canBeModifiedBy(userId: UserId): Boolean {
    return this.publisherId == userId
}

fun PriceAmount.isValid(): Boolean {
    if (this.value.isEmpty()) return true
    return this.value.all { character -> character.isDigit() || character == '.' } &&
            this.value.count { character -> character == '.' } <= 1
}

fun ListingTitle.isValid(): Boolean {
    return this.value.isNotBlank() && this.value.length <= MAXIMUM_TITLE_LENGTH
}

fun ImageUrl.isValid(): Boolean {
    return this.value.isNotBlank()
}

fun List<ImageUrl>.isValid(): Boolean {
    return this.all { it.isValid() }
}