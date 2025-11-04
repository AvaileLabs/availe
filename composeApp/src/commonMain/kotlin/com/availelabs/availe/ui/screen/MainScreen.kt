package com.availelabs.availe.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.ui.components.IMainComponent
import com.availelabs.availe.ui.components.MainUiState
import com.composeunstyled.Button
import com.composeunstyled.Text

@Composable
fun MainScreen(component: IMainComponent) {
    val uiState by component.uiState.subscribeAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Hello, world!")
            Button(
                onClick = component::onLogoutClicked,
                modifier = Modifier.background(Color(0xFFEEEEEE)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                enabled = uiState is MainUiState.Idle
            ) {
                when (uiState) {
                    is MainUiState.Idle -> Text("Logout")
                    is MainUiState.LoggingOut -> Text("Logging out...")
                }
            }
        }
    }
}