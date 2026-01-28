package com.availelabs.availe.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.chat_status_authentication_failed
import availe.client.designsystem.generated.resources.chat_status_connecting
import availe.client.designsystem.generated.resources.chat_status_disconnected
import com.availelabs.availe.repositories.MessagingConnectionState
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectionStatusBanner(status: MessagingConnectionState) {
    val (backgroundColor, textColor, message) = when (status) {
        MessagingConnectionState.Connecting,
        MessagingConnectionState.Handshaking -> Triple(
            Color(0xFFEAB308),
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