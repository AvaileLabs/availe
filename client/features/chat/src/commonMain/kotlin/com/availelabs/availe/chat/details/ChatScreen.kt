package com.availelabs.availe.chat.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.availelabs.availe.domain.model.MessageContent
import com.availelabs.availe.model.ChatMessage
import com.availelabs.availe.ui.components.chat.ChatHeader
import com.availelabs.availe.ui.components.chat.ChatInput
import com.availelabs.availe.ui.components.chat.ConnectionStatusBanner
import com.availelabs.availe.ui.components.chat.MessageBubble
import com.composables.uikit.scaffolds.ScreenScaffold
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.secondary
import com.composeunstyled.theme.Theme

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

    if (isOverlay) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][secondary].copy(alpha = 0.3f))
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            ConnectionStatusBanner(chatState.connectionStatus)

            MessageList(
                messages = chatState.messages,
                listState = lazyListState,
                modifier = Modifier.weight(1f)
            )

            ChatInput(
                onSendMessage = { messageBody ->
                    component.onIntent(ChatStore.Intent.SendMessage(MessageContent(messageBody)))
                },
                enabled = true
            )
        }
    } else {
        ScreenScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][secondary].copy(alpha = 0.3f))
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                ConnectionStatusBanner(chatState.connectionStatus)

                ChatHeader(
                    isOnline = chatState.isOnline,
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
                    },
                    enabled = true
                )
            }
        }
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