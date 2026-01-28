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

sealed interface LastMessageTimestamp {
    data object Unknown : LastMessageTimestamp
    data class Known(val value: Instant) : LastMessageTimestamp
}

@Serializable
@JvmInline
value class IsOnline(val value: Boolean)