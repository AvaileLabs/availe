@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.ColumnAdapter
import com.availelabs.availe.domain.model.CurrencyCode
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.ListingDescription
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingTitle
import com.availelabs.availe.domain.model.PriceAmount
import com.availelabs.availe.domain.model.UserId
import kotlin.time.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object ClientDatabaseColumnAdapters {

    val listingId = object : ColumnAdapter<ListingId, String> {
        override fun decode(databaseValue: String): ListingId = ListingId(Uuid.parse(databaseValue))
        override fun encode(value: ListingId): String = value.value.toString()
    }

    val userId = object : ColumnAdapter<UserId, String> {
        override fun decode(databaseValue: String): UserId = UserId(Uuid.parse(databaseValue))
        override fun encode(value: UserId): String = value.value.toString()
    }

    val listingTitle = object : ColumnAdapter<ListingTitle, String> {
        override fun decode(databaseValue: String): ListingTitle = ListingTitle(databaseValue)
        override fun encode(value: ListingTitle): String = value.value
    }

    val listingDescription = object : ColumnAdapter<ListingDescription, String> {
        override fun decode(databaseValue: String): ListingDescription = ListingDescription(databaseValue)
        override fun encode(value: ListingDescription): String = value.value
    }

    val priceAmount = object : ColumnAdapter<PriceAmount, String> {
        override fun decode(databaseValue: String): PriceAmount = PriceAmount(databaseValue)
        override fun encode(value: PriceAmount): String = value.value
    }

    val currencyCode = object : ColumnAdapter<CurrencyCode, String> {
        override fun decode(databaseValue: String): CurrencyCode = CurrencyCode(databaseValue)
        override fun encode(value: CurrencyCode): String = value.value
    }

    val instant = object : ColumnAdapter<Instant, String> {
        override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)
        override fun encode(value: Instant): String = value.toString()
    }

    val imageLinks = object : ColumnAdapter<List<ImageUrl>, String> {
        private val json = Json { ignoreUnknownKeys = true }
        private val serializer = ListSerializer(ImageUrl.serializer())

        override fun decode(databaseValue: String): List<ImageUrl> {
            return if (databaseValue.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString(serializer, databaseValue)
            }
        }

        override fun encode(value: List<ImageUrl>): String {
            return json.encodeToString(serializer, value)
        }
    }
}