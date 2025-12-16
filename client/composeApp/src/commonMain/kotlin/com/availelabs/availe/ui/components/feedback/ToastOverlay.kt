package com.availelabs.availe.ui.components.feedback

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.domain.model.ToastId
import com.availelabs.availe.domain.model.ToastMessage
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.*
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme

@Composable
fun ToastOverlay(
    messages: List<ToastMessage>,
    onDismiss: (ToastId) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            messages.forEach { message ->
                key(message.id.value) {
                    Toast(
                        message = message,
                        onDismiss = { onDismiss(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Toast(
    message: ToastMessage,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val backgroundColor = Theme[colors][card]
        val textColor = Theme[colors][onCard]

        val (icon, iconColor) = when (message) {
            is ToastMessage.Error -> Lucide.CircleAlert to Theme[colors][destructive]
            is ToastMessage.Success -> Lucide.CircleCheck to Color(0xFF10B981)
            is ToastMessage.Info -> Lucide.Info to Theme[colors][primary]
        }

        Row(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(1.dp, Theme[colors][outline].copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            SelectionContainer(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = message.text.asString(),
                    style = Theme[textStyles][body],
                    color = textColor
                )
            }

            Icon(
                imageVector = Lucide.X,
                contentDescription = "Dismiss",
                tint = Theme[colors][outline],
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}