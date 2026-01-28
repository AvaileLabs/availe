package com.availelabs.availe.chat.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.components.chat.ChatListItem
import com.availelabs.availe.ui.components.chat.ConnectionStatusBanner

@Composable
fun ChatListScreen(
    component: IChatListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        ConnectionStatusBanner(state.connectionStatus)

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