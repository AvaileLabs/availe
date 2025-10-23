@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.domain.listing

import javax.money.MonetaryAmount
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface CommonListingProperties {
    val id: Uuid
    val title: String
    val description: String?
    val price: MonetaryAmount
    val createdAt: Instant
    val imageLinks: List<String>
    val publisherId: Uuid
}

data class CommonListingData(
    override val id: Uuid,
    override val title: String,
    override val description: String?,
    override val price: MonetaryAmount,
    override val createdAt: Instant,
    override val imageLinks: List<String>,
    override val publisherId: Uuid
) : CommonListingProperties