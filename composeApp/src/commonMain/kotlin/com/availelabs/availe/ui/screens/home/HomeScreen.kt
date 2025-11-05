package com.availelabs.availe.ui.screens.home

import ListingGrid
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.ui.events.HomeScreenEvents
import com.availelabs.availe.ui.utils.ContentState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.uikit.components.*

@Composable
internal fun HomeScreen(homeScreenComponent: IHomeScreenComponent) {
    val uiState by homeScreenComponent.uiState.subscribeAsState()

    AppScaffold(
        content = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    SecondaryButton(onClick = { homeScreenComponent.onEvent(HomeScreenEvents.LogoutClicked) }) {
                        Text("Logout")
                    }
                    SecondaryButton(onClick = { homeScreenComponent.onEvent(HomeScreenEvents.Refresh) }) {
                        Icon(Lucide.RefreshCw, "Refresh")
                    }
                }
            )

            when (val state = uiState.listingsState) {
                is ContentState.Idle, is ContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IntermediateProgressIndicator()
                    }
                }

                is ContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}")
                    }
                }

                is ContentState.Content -> {
                    ListingGrid(
                        listings = state.data,
                        onItemClick = { homeScreenComponent.onEvent(HomeScreenEvents.ItemClicked(it)) },
                        onFavoriteClick = { homeScreenComponent.onEvent(HomeScreenEvents.FavoriteClicked(it)) }
                    )
                }
            }
        }
    )
}