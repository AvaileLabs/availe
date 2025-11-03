package com.availelabs.availe.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.components.ILoginComponent
import com.composeunstyled.Button
import com.composeunstyled.Text

@Composable
fun LoginScreen(component: ILoginComponent) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Please log in to continue.")
            Button(
                onClick = component::onLoginClicked,
                modifier = Modifier.background(Color(0xFFEEEEEE)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Login")
            }
        }
    }
}