package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.db.*
import com.availelabs.availe.domain.model.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object ClientDatabaseFactory {
    @OptIn(ExperimentalUuidApi::class)
    fun createClientDatabase(sqlDriver: SqlDriver, json: Json): ClientDatabase {
        val listingIdColumnAdapter = object : ColumnAdapter<ListingId, String> {
            override fun decode(databaseValue: String): ListingId = ListingId(Uuid.parse(databaseValue))
            override fun encode(value: ListingId): String = value.value.toString()
        }

        val userIdColumnAdapter = object : ColumnAdapter<UserId, String> {
            override fun decode(databaseValue: String): UserId = UserId(Uuid.parse(databaseValue))
            override fun encode(value: UserId): String = value.value.toString()
        }

        val emailColumnAdapter = object : ColumnAdapter<Email, String> {
            override fun decode(databaseValue: String): Email = Email(databaseValue)
            override fun encode(value: Email): String = value.value
        }

        val listingCreationTimeColumnAdapter = object : ColumnAdapter<ListingCreationTime, String> {
            override fun decode(databaseValue: String): ListingCreationTime =
                ListingCreationTime(Instant.parse(databaseValue))

            override fun encode(value: ListingCreationTime): String = value.value.toString()
        }

        val listingCategoryColumnAdapter = object : ColumnAdapter<ListingCategory, String> {
            override fun decode(databaseValue: String): ListingCategory = ListingCategory.valueOf(databaseValue)
            override fun encode(value: ListingCategory): String = value.name
        }

        val listingTitleColumnAdapter = object : ColumnAdapter<ListingTitle, String> {
            override fun decode(databaseValue: String): ListingTitle = ListingTitle(databaseValue)
            override fun encode(value: ListingTitle): String = value.value
        }

        val listingDescriptionColumnAdapter = object : ColumnAdapter<ListingDescription, String> {
            override fun decode(databaseValue: String): ListingDescription = ListingDescription(databaseValue)
            override fun encode(value: ListingDescription): String = value.value
        }

        val priceAmountColumnAdapter = object : ColumnAdapter<PriceAmount, String> {
            override fun decode(databaseValue: String): PriceAmount = PriceAmount(databaseValue)
            override fun encode(value: PriceAmount): String = value.value
        }

        val currencyCodeColumnAdapter = object : ColumnAdapter<CurrencyCode, String> {
            override fun decode(databaseValue: String): CurrencyCode = CurrencyCode(databaseValue)
            override fun encode(value: CurrencyCode): String = value.value
        }

        val imageListColumnAdapter = object : ColumnAdapter<List<ImageUrl>, String> {
            override fun decode(databaseValue: String): List<ImageUrl> =
                json.decodeFromString(ListSerializer(ImageUrl.serializer()), databaseValue)

            override fun encode(value: List<ImageUrl>): String =
                json.encodeToString(ListSerializer(ImageUrl.serializer()), value)
        }

        return ClientDatabase(
            driver = sqlDriver,
            listing_event_detailsAdapter = Listing_event_details.Adapter(
                listing_idAdapter = listingIdColumnAdapter
            ),
            listing_item_detailsAdapter = Listing_item_details.Adapter(
                listing_idAdapter = listingIdColumnAdapter
            ),
            listing_service_detailsAdapter = Listing_service_details.Adapter(
                listing_idAdapter = listingIdColumnAdapter
            ),
            listingsAdapter = Listings.Adapter(
                primary_key_idAdapter = listingIdColumnAdapter,
                created_atAdapter = listingCreationTimeColumnAdapter,
                publisher_idAdapter = userIdColumnAdapter,
                publisher_emailAdapter = emailColumnAdapter,
                listing_typeAdapter = listingCategoryColumnAdapter,
                titleAdapter = listingTitleColumnAdapter,
                descriptionAdapter = listingDescriptionColumnAdapter,
                price_amountAdapter = priceAmountColumnAdapter,
                price_currencyAdapter = currencyCodeColumnAdapter,
                image_linksAdapter = imageListColumnAdapter
            )
        )
    }
}