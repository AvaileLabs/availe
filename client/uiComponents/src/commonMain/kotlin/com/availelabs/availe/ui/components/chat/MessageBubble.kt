package com.availelabs.availe.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.model.MessageStatus
import com.composables.icons.lucide.*
import com.composables.uikit.components.Avatar
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    val bubbleColor = if (isMine) Theme[colors][primary] else Theme[colors][card]
    val contentColor = if (isMine) Theme[colors][onPrimary] else Theme[colors][onCard]

    val bubbleShape = if (isMine) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Avatar(
                painter = rememberVectorPainter(Lucide.User),
                initials = "T",
                modifier = Modifier.size(28.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .dropShadow(shape = bubbleShape, shadow = Theme[shadows][subtle])
                    .background(bubbleColor, bubbleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.content.value,
                        color = contentColor,
                        lineHeight = 20.sp
                    )
                }
            }

            if (isMine) {
                Spacer(modifier = Modifier.height(2.dp))
                MessageStatusIcon(message.status)
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    val icon = when (status) {
        MessageStatus.Pending -> Lucide.Clock
        MessageStatus.Sent -> Lucide.Check
        MessageStatus.Delivered -> Lucide.CheckCheck
        MessageStatus.Read -> Lucide.CheckCheck
        MessageStatus.Failed -> Lucide.CircleX
    }

    val tint = when (status) {
        MessageStatus.Read -> Color(0xFF3B82F6)
        MessageStatus.Failed -> Theme[colors][destructive]
        else -> Theme[colors][outline].copy(alpha = 0.6f)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(12.dp),
        tint = tint
    )
}