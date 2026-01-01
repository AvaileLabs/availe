package com.availelabs.availe.model

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.domain.model.UserDisplayName

data class ChatConversation(
    val jid: Jid,
    val displayName: UserDisplayName,
    val avatarUrl: ImageUrl?,
    val lastMessage: MessageContent,
    val lastMessageTimestamp: LastMessageTimestamp,
    val unreadCount: UnreadCount,
    val isOnline: IsOnline
)