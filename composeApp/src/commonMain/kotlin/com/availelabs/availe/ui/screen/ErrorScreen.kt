package com.availelabs.availe.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.components.IErrorComponent
import com.composeunstyled.Text

@Composable
fun ErrorScreen(component: IErrorComponent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Initialization Failed", color = Color(0xFFB00020))
            Text(component.message, color = Color(0xFFB00020))
            Text("Please restart the application.")
        }
    }
}