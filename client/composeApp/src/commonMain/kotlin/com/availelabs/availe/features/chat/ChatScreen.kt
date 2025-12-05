package com.availelabs.availe.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.uikit.components.AppScaffold
import com.composables.uikit.components.PrimaryButton
import com.composeunstyled.Text

@Composable
fun ChatScreen(component: IChatComponent) {
    val state by component.state.collectAsState()

    AppScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Chat Screen (User: test)")
            Text("Status: ${state.connectionStatus}")

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { msg ->
                    Text(
                        text = msg,
                        modifier = Modifier
                            .background(if (msg.startsWith("Me:")) Color.LightGray else Color.Cyan)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(onClick = { component.onIntent(ChatStore.Intent.SendMessage("Hello from KMP!")) }) {
                Text("Send Hello")
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(onClick = { component.onBackClicked() }) {
                Text("Leave Chat")
            }
        }
    }
}