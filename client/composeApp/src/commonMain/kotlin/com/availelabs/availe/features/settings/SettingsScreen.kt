package com.availelabs.availe.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.availelabs.availe.ui.ScreenScaffold
import com.composables.uikit.components.Text
import com.composables.uikit.components.TopAppBar

@Composable
fun SettingsScreen() {
    ScreenScaffold(
        appBar = { TopAppBar(title = { Text("Settings") }) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Settings Screen")
        }
    }
}