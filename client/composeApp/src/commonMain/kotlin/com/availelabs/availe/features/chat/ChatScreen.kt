package com.availelabs.availe.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.composeapp.generated.resources.*
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.repositories.MessagingConnectionState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.User
import com.composables.uikit.components.*
import com.composables.uikit.scaffolds.ScreenScaffold
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatScreen(
    component: IChatComponent,
    isOverlay: Boolean = false
) {
    val chatState by component.state.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    // If it's an overlay, we don't need the ScreenScaffold because the Window component handles the container
    if (isOverlay) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][secondary].copy(alpha = 0.3f))
        ) {
            MessageList(
                messages = chatState.messages,
                listState = lazyListState,
                modifier = Modifier.weight(1f)
            )

            ChatInput(
                onSendMessage = { messageBody ->
                    component.onIntent(ChatStore.Intent.SendMessage(MessageContent(messageBody)))
                }
            )
        }
    } else {
        ScreenScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][secondary].copy(alpha = 0.3f))
            ) {
                UserInfoHeader(
                    connectionStatus = chatState.connectionStatus,
                    recipientName = chatState.recipientJid.value
                )

                MessageList(
                    messages = chatState.messages,
                    listState = lazyListState,
                    modifier = Modifier.weight(1f)
                )

                ChatInput(
                    onSendMessage = { messageBody ->
                        component.onIntent(ChatStore.Intent.SendMessage(MessageContent(messageBody)))
                    }
                )
            }
        }
    }
}

@Composable
private fun UserInfoHeader(
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
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = Theme[colors][outline].copy(alpha = 0.8f)
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(messages) { chatMessage ->
            MessageBubble(
                message = chatMessage,
                isMine = chatMessage.isMine
            )
        }
    }
}

@Composable
private fun MessageBubble(
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
    }
}

@Composable
private fun ChatInput(
    onSendMessage: (String) -> Unit
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
                shape = RoundedCornerShape(percent = 50)
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