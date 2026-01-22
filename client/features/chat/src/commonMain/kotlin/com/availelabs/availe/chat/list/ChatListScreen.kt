package com.availelabs.availe.chat.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.chat_status_authentication_failed
import availe.client.designsystem.generated.resources.chat_status_connecting
import availe.client.designsystem.generated.resources.chat_status_disconnected
import coil3.compose.AsyncImage
import com.availelabs.availe.model.ChatConversation
import com.availelabs.availe.repositories.MessagingConnectionState
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatListScreen(
    component: IChatListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        ConnectionStatusHeader(state.connectionStatus)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.conversations, key = { it.jid.value }) { conversation ->
                ChatListItem(
                    conversation = conversation,
                    isSelected = conversation.jid == state.selectedJid,
                    onClick = { component.onConversationSelected(conversation.jid) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusHeader(status: MessagingConnectionState) {
    val (backgroundColor, textColor, message) = when (status) {
        MessagingConnectionState.Connecting,
        MessagingConnectionState.Handshaking -> Triple(
            Color(0xFFEAB308), // Yellow-500
            Color.Black,
            stringResource(Res.string.chat_status_connecting)
        )

        is MessagingConnectionState.Error -> Triple(
            Theme[colors][destructive],
            Theme[colors][onDestructive],
            status.message
        )

        MessagingConnectionState.AuthenticationFailed -> Triple(
            Theme[colors][destructive],
            Theme[colors][onDestructive],
            stringResource(Res.string.chat_status_authentication_failed)
        )

        MessagingConnectionState.Disconnected -> Triple(
            Theme[colors][outline],
            Theme[colors][onBackground],
            stringResource(Res.string.chat_status_disconnected)
        )

        MessagingConnectionState.Authenticated -> Triple(Color.Transparent, Color.Transparent, "")
    }

    AnimatedVisibility(
        visible = status !is MessagingConnectionState.Authenticated,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 4.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            SelectionContainer {
                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ChatListItem(
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
        // Avatar Area
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
                        .background(Color(0xFF10B981)) // Green
                        .align(Alignment.BottomEnd)
                        .background(Theme[colors][onBackground], CircleShape) // Border simulation
                        .padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Area
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

                // Mock time for now, can format properly later
                Text(
                    text = "10:30 AM",
                    fontSize = 12.sp,
                    color = Theme[colors][onBackground].copy(alpha = 0.6f)
                )
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