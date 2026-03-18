@file:OptIn(ExperimentalTime::class)

package com.availelabs.availe.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.db.*
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.model.ConversationType
import com.availelabs.availe.model.MessageStatus
import com.availelabs.availe.model.MessageType
import com.availelabs.availe.model.UnreadCount
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object ClientDatabaseFactory {
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

        val jidColumnAdapter = object : ColumnAdapter<Jid, String> {
            override fun decode(databaseValue: String): Jid = Jid(databaseValue)
            override fun encode(value: Jid): String = value.value
        }

        val conversationTypeColumnAdapter = object : ColumnAdapter<ConversationType, String> {
            override fun decode(databaseValue: String): ConversationType = ConversationType.valueOf(databaseValue)
            override fun encode(value: ConversationType): String = value.name
        }

        val userDisplayNameColumnAdapter = object : ColumnAdapter<UserDisplayName, String> {
            override fun decode(databaseValue: String): UserDisplayName = UserDisplayName(databaseValue)
            override fun encode(value: UserDisplayName): String = value.value
        }

        val messageContentColumnAdapter = object : ColumnAdapter<MessageContent, String> {
            override fun decode(databaseValue: String): MessageContent = MessageContent(databaseValue)
            override fun encode(value: MessageContent): String = value.value
        }

        val instantLongColumnAdapter = object : ColumnAdapter<Instant, Long> {
            override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)
            override fun encode(value: Instant): Long = value.toEpochMilliseconds()
        }

        val unreadCountColumnAdapter = object : ColumnAdapter<UnreadCount, Long> {
            override fun decode(databaseValue: Long): UnreadCount = UnreadCount(databaseValue.toInt())
            override fun encode(value: UnreadCount): Long = value.value.toLong()
        }

        val chatMessageIdColumnAdapter = object : ColumnAdapter<ChatMessageId, String> {
            override fun decode(databaseValue: String): ChatMessageId = ChatMessageId(databaseValue)
            override fun encode(value: ChatMessageId): String = value.value
        }

        val messageTypeColumnAdapter = object : ColumnAdapter<MessageType, String> {
            override fun decode(databaseValue: String): MessageType = MessageType.valueOf(databaseValue)
            override fun encode(value: MessageType): String = value.name
        }

        val messageStatusColumnAdapter = object : ColumnAdapter<MessageStatus, String> {
            override fun decode(databaseValue: String): MessageStatus = MessageStatus.valueOf(databaseValue)
            override fun encode(value: MessageStatus): String = value.name
        }

        val localDateTimeColumnAdapter = object : ColumnAdapter<LocalDateTime, String> {
            override fun decode(databaseValue: String): LocalDateTime = LocalDateTime.parse(databaseValue)
            override fun encode(value: LocalDateTime): String = value.toString()
        }

        val timeZoneIdentifierColumnAdapter = object : ColumnAdapter<TimeZoneIdentifier, String> {
            override fun decode(databaseValue: String): TimeZoneIdentifier = TimeZoneIdentifier(databaseValue)
            override fun encode(value: TimeZoneIdentifier): String = value.value
        }

        return ClientDatabase.Companion(
            driver = sqlDriver,
            chat_conversationAdapter = Chat_conversation.Adapter(
                jidAdapter = jidColumnAdapter,
                typeAdapter = conversationTypeColumnAdapter,
                display_nameAdapter = userDisplayNameColumnAdapter,
                last_message_contentAdapter = messageContentColumnAdapter,
                last_message_timestampAdapter = instantLongColumnAdapter,
                unread_countAdapter = unreadCountColumnAdapter
            ),
            chat_messageAdapter = Chat_message.Adapter(
                idAdapter = chatMessageIdColumnAdapter,
                conversation_jidAdapter = jidColumnAdapter,
                sender_jidAdapter = jidColumnAdapter,
                sender_display_nameAdapter = userDisplayNameColumnAdapter,
                contentAdapter = messageContentColumnAdapter,
                typeAdapter = messageTypeColumnAdapter,
                statusAdapter = messageStatusColumnAdapter,
                timestampAdapter = instantLongColumnAdapter
            ),
            listing_event_detailsAdapter = Listing_event_details.Adapter(
                listing_idAdapter = listingIdColumnAdapter,
                start_local_timeAdapter = localDateTimeColumnAdapter,
                end_local_timeAdapter = localDateTimeColumnAdapter,
                start_instantAdapter = instantLongColumnAdapter,
                end_instantAdapter = instantLongColumnAdapter,
                time_zone_identifierAdapter = timeZoneIdentifierColumnAdapter
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