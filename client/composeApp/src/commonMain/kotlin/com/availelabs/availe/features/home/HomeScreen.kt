package com.availelabs.availe.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.composables.uikit.components.Text

@Composable
internal fun HomeScreen(homeScreenComponent: IHomeComponent) {
    val uiState by homeScreenComponent.uiState.subscribeAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home Screen")
    }
}