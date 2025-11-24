package com.availelabs.availe.features.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.availelabs.availe.ui.ScreenScaffold
import com.composables.uikit.components.Text
import com.composables.uikit.components.TopAppBar

@Composable
fun FavoritesScreen() {
    ScreenScaffold(
        appBar = { TopAppBar(title = { Text("Favorites") }) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Favorites Screen")
        }
    }
}