package com.availelabs.availe.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.repositories.MessagingConnectionState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.uikit.components.Avatar
import com.composables.uikit.components.HorizontalDivider
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatHeader(
    connectionStatus: MessagingConnectionState,
    recipientName: String
) {
    val isConnected = connectionStatus is MessagingConnectionState.Authenticated
    val statusColor = if (isConnected) Color(0xFF10B981) else Theme[colors][destructive]

    val statusText = when (connectionStatus) {
        MessagingConnectionState.Authenticated -> stringResource(Res.string.chat_status_online)
        MessagingConnectionState.AuthenticationFailed -> stringResource(Res.string.chat_status_authentication_failed)
        MessagingConnectionState.Connecting -> stringResource(Res.string.chat_status_connecting)
        MessagingConnectionState.Disconnected -> stringResource(Res.string.chat_status_disconnected)
        is MessagingConnectionState.Error -> connectionStatus.message
        MessagingConnectionState.Handshaking -> stringResource(Res.string.chat_status_handshaking)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][card])
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                painter = rememberVectorPainter(Lucide.User),
                initials = recipientName.take(1).uppercase(),
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = recipientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SelectionContainer {
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = Theme[colors][outline].copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}