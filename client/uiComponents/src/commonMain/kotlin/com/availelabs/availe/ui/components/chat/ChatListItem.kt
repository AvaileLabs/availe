package com.availelabs.availe.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.availelabs.availe.model.ChatConversation
import com.availelabs.availe.model.LastMessageTimestamp
import com.composables.uikit.components.Text
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composables.uikit.styling.outline
import com.composables.uikit.styling.primary
import com.composeunstyled.theme.Theme

@Composable
fun ChatListItem(
    conversation: ChatConversation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Theme[colors][outline].copy(alpha = 0.1f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp)
        ) {
            val avatarUrl = conversation.avatarUrl
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl.value,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Theme[colors][outline].copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.displayName.value.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Theme[colors][onBackground]
                    )
                }
            }

            if (conversation.isOnline.value) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .align(Alignment.BottomEnd)
                        .background(Theme[colors][onBackground], CircleShape)
                        .padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.displayName.value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val timeText = when (val time = conversation.lastMessageTimestamp) {
                    is LastMessageTimestamp.Known -> "Now"
                    LastMessageTimestamp.Unknown -> ""
                }

                if (timeText.isNotEmpty()) {
                    Text(
                        text = timeText,
                        fontSize = 12.sp,
                        color = Theme[colors][onBackground].copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage.value,
                    fontSize = 14.sp,
                    color = if (conversation.unreadCount.value > 0) Theme[colors][onBackground] else Theme[colors][onBackground].copy(
                        alpha = 0.6f
                    ),
                    fontWeight = if (conversation.unreadCount.value > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount.value > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 20.dp)
                            .clip(CircleShape)
                            .background(Theme[colors][primary])
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.value.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}