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
import com.availelabs.availe.ui.RefreshableScreenScaffold
import com.availelabs.availe.ui.components.RefreshIcon
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.uikit.components.*

@Composable
internal fun HomeScreen(
    homeScreenComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onPostClick: () -> Unit
) {
    val uiState by homeScreenComponent.uiState.subscribeAsState()

    RefreshableScreenScaffold(
        state = uiState.listingsState,
        onRefresh = { homeScreenComponent.onEvent(HomeEvents.Refresh) },
        appBar = {
            TopAppBar(
                title = { Text("Availe") },
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        Icon(Lucide.Bell, "Notifications")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Lucide.Search, "Search")
                    }
                    IconButton(onClick = onPostClick) {
                        Icon(Lucide.CirclePlus, "Post")
                    }
                }
            )
        }
    ) { listings, refreshState ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(listOf("For Sale", "Services", "Housing")) { category ->
                            Chip {
                                Text(category)
                            }
                        }
                    }

                    IconButton(onClick = { homeScreenComponent.onEvent(HomeEvents.Refresh) }) {
                        RefreshIcon(refreshState)
                    }
                }
            }

            items(listings) { item ->
                ListingCard(
                    item = item,
                    onItemClick = { homeScreenComponent.onEvent(HomeEvents.ItemClicked(it)) },
                    onFavoriteClick = { homeScreenComponent.onEvent(HomeEvents.FavoriteClicked(it)) }
                )
            }
        }
    }
}