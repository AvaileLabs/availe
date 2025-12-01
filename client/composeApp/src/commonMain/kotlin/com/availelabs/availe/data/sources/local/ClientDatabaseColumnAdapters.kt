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

object ListingIdAdapter : ColumnAdapter<ListingId, String> {
    override fun decode(databaseValue: String): ListingId = ListingId(Uuid.parse(databaseValue))
    override fun encode(value: ListingId): String = value.value.toString()
}

object UserIdAdapter : ColumnAdapter<UserId, String> {
    override fun decode(databaseValue: String): UserId = UserId(Uuid.parse(databaseValue))
    override fun encode(value: UserId): String = value.value.toString()
}

object ListingTitleAdapter : ColumnAdapter<ListingTitle, String> {
    override fun decode(databaseValue: String): ListingTitle = ListingTitle(databaseValue)
    override fun encode(value: ListingTitle): String = value.value
}

object ListingDescriptionAdapter : ColumnAdapter<ListingDescription, String> {
    override fun decode(databaseValue: String): ListingDescription = ListingDescription(databaseValue)
    override fun encode(value: ListingDescription): String = value.value
}

object PriceAmountAdapter : ColumnAdapter<PriceAmount, String> {
    override fun decode(databaseValue: String): PriceAmount = PriceAmount(databaseValue)
    override fun encode(value: PriceAmount): String = value.value
}

object CurrencyCodeAdapter : ColumnAdapter<CurrencyCode, String> {
    override fun decode(databaseValue: String): CurrencyCode = CurrencyCode(databaseValue)
    override fun encode(value: CurrencyCode): String = value.value
}

object InstantAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)
    override fun encode(value: Instant): String = value.toString()
}

object ImageLinksAdapter : ColumnAdapter<List<ImageUrl>, String> {
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