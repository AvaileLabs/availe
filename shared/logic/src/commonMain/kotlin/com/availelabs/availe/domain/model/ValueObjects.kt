@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ListingTitle(val value: String)

@Serializable
@JvmInline
value class ListingDescription(val value: String)

@Serializable
@JvmInline
value class PriceAmount(val value: String)

@Serializable
@JvmInline
value class CurrencyCode(val value: String)

@Serializable
@JvmInline
value class ImageUrl(val value: String)

@Serializable
@JvmInline
value class Email(val value: String)

@Serializable
@JvmInline
value class UserId(val value: Uuid)

@Serializable
@JvmInline
value class ListingId(val value: Uuid)

@Serializable
@JvmInline
value class ListingCreationTime(val value: Instant)

@Serializable
@JvmInline
value class Jid(val value: String)

@Serializable
@JvmInline
value class AuthToken(val value: String)

@Serializable
@JvmInline
value class ChatMessageId(val value: String)

@Serializable
@JvmInline
value class MessageContent(val value: String)