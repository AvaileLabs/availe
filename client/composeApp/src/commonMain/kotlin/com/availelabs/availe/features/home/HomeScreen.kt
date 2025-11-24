package com.availelabs.availe.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.home.components.ListingCard
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.ui.utils.ContentState
import com.composables.uikit.components.Chip
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.Text

@Composable
internal fun HomeScreen(homeScreenComponent: IHomeComponent) {
    val uiState by homeScreenComponent.uiState.subscribeAsState()

    ScreenScaffold {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("For Sale", "Services", "Housing")) { category ->
                        Chip {
                            Text(category)
                        }
                    }
                }
            }

            val listingsState = uiState.listingsState
            when (listingsState) {
                is ContentState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IntermediateProgressIndicator()
                        }
                    }
                }

                is ContentState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listingsState.message)
                        }
                    }
                }

                is ContentState.Content -> {
                    items(listingsState.data) { item ->
                        ListingCard(
                            item = item,
                            onItemClick = { homeScreenComponent.onEvent(HomeEvents.ItemClicked(it)) },
                            onFavoriteClick = { homeScreenComponent.onEvent(HomeEvents.FavoriteClicked(it)) }
                        )
                    }
                }

                is ContentState.Refreshing -> {
                    items(listingsState.data) { item ->
                        ListingCard(
                            item = item,
                            onItemClick = { homeScreenComponent.onEvent(HomeEvents.ItemClicked(it)) },
                            onFavoriteClick = { homeScreenComponent.onEvent(HomeEvents.FavoriteClicked(it)) }
                        )
                    }
                }

                is ContentState.Idle -> {
                }
            }
        }
    }
}