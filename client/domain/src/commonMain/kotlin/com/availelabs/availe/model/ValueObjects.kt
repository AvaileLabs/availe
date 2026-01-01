package com.availelabs.availe.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@Serializable
@JvmInline
value class DisplayMessage(val value: String)

@Serializable
@JvmInline
value class UnreadCount(val value: Int)

@Serializable
@JvmInline
value class LastMessageTimestamp(val value: Instant)

@Serializable
@JvmInline
value class IsOnline(val value: Boolean)