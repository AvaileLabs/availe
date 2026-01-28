package com.availelabs.availe.mapper

import com.availelabs.availe.db.Chat_conversation
import com.availelabs.availe.db.Chat_message
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatConversation
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.model.IsOnline
import com.availelabs.availe.model.LastMessageTimestamp

internal fun Chat_conversation.toDomain(): ChatConversation {
    val timestamp = this.last_message_timestamp
    val domainTimestamp = if (timestamp != null) {
        LastMessageTimestamp.Known(timestamp)
    } else {
        LastMessageTimestamp.Unknown
    }

    return ChatConversation(
        jid = this.jid,
        displayName = this.display_name,
        avatarUrl = null, // Not persisted in DB yet
        lastMessage = this.last_message_content ?: MessageContent(""),
        lastMessageTimestamp = domainTimestamp,
        unreadCount = this.unread_count,
        isOnline = IsOnline(false) // Presence is transient, defaulted to false
    )
}

internal fun Chat_message.toDomain(): ChatMessage {
    return ChatMessage(
        id = this.id,
        content = this.content,
        senderJid = this.sender_jid,
        timestamp = this.timestamp,
        isMine = this.is_mine,
        status = this.status
    )
}