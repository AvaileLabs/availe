package com.availelabs.availe.ui.screens.home

import com.availelabs.availe.ui.utils.ContentState

data class ListingItem(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String,
    val isFavorited: Boolean = false
)

data class HomeUiState(
    val listingsState: ContentState<List<ListingItem>> = ContentState.Idle
)