package com.availelabs.availe.domain.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ChatMessage(
    val id: String = Uuid.random().toString(),
    val content: String,
    val senderId: String,
    val timestamp: Instant = Clock.System.now(),
    val isMine: Boolean
)