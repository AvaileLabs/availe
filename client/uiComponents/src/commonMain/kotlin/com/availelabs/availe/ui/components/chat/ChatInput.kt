package com.availelabs.availe.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.accessibility_action_send
import availe.client.designsystem.generated.resources.chat_input_placeholder
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.uikit.components.*
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onPrimary
import com.composables.uikit.styling.primary
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatInput(
    onSendMessage: (String) -> Unit,
    enabled: Boolean
) {
    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][card])
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = messageInput,
                onValueChange = { newValue -> messageInput = newValue },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.isShiftPressed) {
                                false
                            } else {
                                if (messageInput.isNotBlank()) {
                                    onSendMessage(messageInput)
                                    messageInput = ""
                                }
                                true
                            }
                        } else {
                            false
                        }
                    },
                placeholder = { Text(stringResource(Res.string.chat_input_placeholder)) },
                shape = RoundedCornerShape(percent = 50),
                editable = true
            )

            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput)
                        messageInput = ""
                    }
                },
                enabled = messageInput.isNotBlank(),
                backgroundColor = Theme[colors][primary],
                contentColor = Theme[colors][onPrimary],
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Lucide.Send, contentDescription = stringResource(Res.string.accessibility_action_send))
            }
        }
    }
}