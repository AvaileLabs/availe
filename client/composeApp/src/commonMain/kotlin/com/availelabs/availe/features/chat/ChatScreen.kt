package com.availelabs.availe.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.uikit.components.AppScaffold
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.TextField
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.primary
import com.composables.uikit.styling.secondary
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
fun ChatScreen(component: IChatComponent) {
    val state by component.state.collectAsState()
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    AppScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme[colors][secondary])
                    .padding(16.dp)
            ) {
                Text("Chat with User: test")
                Text(
                    text = state.connectionStatus,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(state.messages) { msg ->
                    val alignment = if (msg.isMine) Alignment.CenterEnd else Alignment.CenterStart
                    val backgroundColor =
                        if (msg.isMine) Theme[colors][primary] else Theme[colors][secondary].copy(alpha = 0.5f)
                    val textColor = if (msg.isMine) Color.White else Color.Black

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .background(backgroundColor, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme[colors][secondary].copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            component.onIntent(ChatStore.Intent.SendMessage(messageInput))
                            messageInput = ""
                        }
                    },
                    enabled = messageInput.isNotBlank()
                ) {
                    Icon(Lucide.Send, contentDescription = "Send")
                }
            }
        }
    }
}