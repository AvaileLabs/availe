package com.availelabs.availe.domain.extensions

import com.availelabs.availe.domain.model.*
import kotlin.uuid.ExperimentalUuidApi

private const val MAXIMUM_TITLE_LENGTH = 100

@OptIn(ExperimentalUuidApi::class)
fun Listing.canBeModifiedBy(userId: UserId): Boolean {
    if (this.publisher.id == userId) return true

    val userIdString = userId.value.toString()
    return this.publisher.jid.value.startsWith(userIdString, ignoreCase = true)
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

fun ListingLocation.isValid(): Boolean {
    return this.value.isNotBlank()
}