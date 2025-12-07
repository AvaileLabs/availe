package com.availelabs.availe.domain.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ChatMessage(
    val id: ChatMessageId = ChatMessageId(Uuid.random().toString()),
    val content: MessageContent,
    val senderJid: Jid,
    val timestamp: Instant = Clock.System.now(),
    val isMine: Boolean
)